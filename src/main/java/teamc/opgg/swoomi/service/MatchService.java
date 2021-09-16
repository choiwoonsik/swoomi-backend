package teamc.opgg.swoomi.service;

import com.merakianalytics.orianna.Orianna;
import com.merakianalytics.orianna.types.common.Region;
import com.merakianalytics.orianna.types.core.searchable.SearchableList;
import com.merakianalytics.orianna.types.core.spectator.CurrentMatch;
import com.merakianalytics.orianna.types.core.spectator.Player;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import teamc.opgg.swoomi.advice.exception.CSummonerNotFoundException;
import teamc.opgg.swoomi.advice.exception.CSummonerNotInGameException;
import teamc.opgg.swoomi.dto.*;
import teamc.opgg.swoomi.entity.ChampionItem;
import teamc.opgg.swoomi.entity.MatchTeamCodeSummoner;
import teamc.opgg.swoomi.repository.ChampionHasItemRepo;
import teamc.opgg.swoomi.repository.ChampionItemRepository;
import teamc.opgg.swoomi.repository.MatchTeamCodeSummonerRepository;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class MatchService {

    @Autowired
    CommonService commonService;
    @Autowired
    private ChampionItemRepository championItemRepository;
    @Autowired
    private MatchTeamCodeSummonerRepository matchTeamCodeSummonerRepository;

    @Transactional(readOnly = true)
    public MatchDto getMatchStatus(String summonerName) {
        MatchDto dto = new MatchDto();
        Summoner summoner = Orianna.summonerNamed(summonerName).withRegion(Region.KOREA).get();

        if (summoner.exists()) {
            dto.setMatchStatus(Orianna.currentMatchForSummoner(summoner).get().exists());
        } else {
            log.info("NO SUMMONER NAMED : " + summonerName);
            throw new CSummonerNotFoundException();
        }
        return dto;
    }

    @Transactional(readOnly = true)
    public MatchDto getMatchStatusByMatchTeamCode(String matchTeamCode) {
        String summonerName = matchTeamCodeSummonerRepository.findFirstByMatchTeamCode(matchTeamCode)
                .orElseThrow(CSummonerNotInGameException::new)
                .getSummonerName();
        return getMatchStatus(summonerName);
    }

//    @Transactional(readOnly = true)
//    public List<PlayerDto> getOpData(String data, boolean flag) {
//        String summonerName = data;
//        if (flag) {
//            summonerName = matchTeamCodeSummonerRepository.findFirstByMatchTeamCode(data).get().getSummonerName();
//        }
//        Summoner summoner = Orianna.summonerNamed(summonerName).withRegion(Region.KOREA).get();
//        if (!summoner.exists()) {
//            log.info("NO SUMMONER NAMED : " + summonerName);
//            throw new CSummonerNotFoundException();
//        }
//        if (!summoner.isInGame()) {
//            log.info("SUMMONER '" + summonerName + "' NOT IN GAME");
//            throw new CSummonerNotInGameException();
//        }
//        List<PlayerDto> playerDtos = new ArrayList<>();
//
//        // 1. 상대팀 구하기
//        SearchableList<Player> sList = summoner.getCurrentMatch().getParticipants();
//        String finalSummonerName = summonerName;
//        Player tempPlayer = sList.find((playerName) -> playerName.getSummoner().getName().equals(finalSummonerName));
//        String teamId = tempPlayer.getTeam().toString();
//
//        // 2. 상대팀 멤버 구하기
//        SearchableList<Player> opList = sList.filter((player) -> !player.getTeam().toString().equals(teamId));
//
//        commonService.initChampionNameHasItem();
//
//        for (Player p : opList) {
//            String championNameforDto = p.getChampion().getName();
//            String championName = championNameforDto.replace(" ", "");
//            Set<String> set = new HashSet<>();
//            Optional<List<ChampionItem>> optionalChampionItems = championItemRepository.findAllByChampionName(championName);
//            List<ItemDto> list = new ArrayList<>();
//            if (ChampionHasItemRepo.getInstance().getChampionSet().contains(championName)
//                    && optionalChampionItems.isPresent()) {
//                list = optionalChampionItems.get()
//                        .stream()
//                        .map((e) -> {
//                                    if (!set.contains(e.getItemName())) {
//                                        set.add(e.getItemName());
//                                        return e.toDto();
//                                    } else {
//                                        return null;
//                                    }
//                                }
//                        )
//                        .filter(Objects::nonNull)
//                        .collect(Collectors.toList());
//            } else {
//                list.add(
//                        ItemDto.builder()
//                                .name("명석함의 아이오니아 장화")
//                                .englishName("Ionian Boots of Lucidity")
//                                .skillAccel("20")
//                                .src("https://opgg-static.akamaized.net/images/lol/item/3158.png?image=q_auto:best&v=1628647804")
//                                .build()
//                );
//            }
//            PlayerDto dto = PlayerDto.builder().summonerName(p.getSummoner().getName())
//                    .championName(championNameforDto)
//                    .championImgUrl(p.getChampion().getImage().getURL())
//                    .ultImgUrl(p.getChampion().getSpells().get(3).getImage().getURL())
//                    .spellDName(p.getSummonerSpellD().getName())
//                    .spellFName(p.getSummonerSpellF().getName())
//                    .spellDImgUrl(p.getSummonerSpellD().getImage().getURL())
//                    .spellFImgUrl(p.getSummonerSpellF().getImage().getURL())
//                    .frequentItems(list)
//                    .build();
//
//            playerDtos.add(dto);
//        }
//        return playerDtos;
//    }

    @Transactional
    public MatchStatusDto getMatchTeamCode(String summonerName) {

        boolean isMyTeam = false;
        long myTeam = 100;
        Summoner summoner;
        MatchStatusDto matchStatusDto;

        summoner = Orianna.summonerNamed(summonerName).withRegion(Region.KOREA).get();
        if (!summoner.exists()) {
            throw new CSummonerNotFoundException();
        }

        CurrentMatch currentMatch = Orianna.currentMatchForSummoner(summoner).get();
        if (currentMatch.getId() != 0) {
            matchStatusDto = MatchStatusDto.builder()
                    .isStarted(false)
                    .matchTeamCode("")
                    .build();
            for (int i = 0; i < 5; i++) {
                Player player = currentMatch.getParticipants().get(i);
                if (player.getSummoner().getName().equals(summonerName)) {
                    isMyTeam = true;
                    break;
                }
            }
            if (!isMyTeam) myTeam = 200;
            String matchTeamCode = String.valueOf(currentMatch.getId() * 1000 + myTeam);
            matchStatusDto.setIsStarted(true);
            matchStatusDto.setMatchTeamCode(matchTeamCode);

            if (matchTeamCodeSummonerRepository.findBySummonerName(summonerName).isPresent()) {
                matchTeamCodeSummonerRepository.findBySummonerName(summonerName).get()
                        .setMatchTeamCode(matchTeamCode);
            } else {
                MatchTeamCodeSummoner matchTeamCodeSummoner = MatchTeamCodeSummoner.builder()
                        .matchTeamCode(matchTeamCode)
                        .summonerName(summonerName)
                        .build();
                matchTeamCodeSummonerRepository.save(matchTeamCodeSummoner);
            }
            return matchStatusDto;
        } else throw new CSummonerNotInGameException();
    }

    @Transactional(readOnly = true)
    public String getMyMatchTeamCodeByEnemy(String summonerName) {

        StringBuilder myCode = new StringBuilder(getMatchTeamCode(summonerName).getMatchTeamCode());
        if (Integer.parseInt(myCode.substring(myCode.length() - 3)) == 100) {
            myCode.replace(myCode.length() - 3, myCode.length(), "200");
        } else {
            myCode.replace(myCode.length() - 3, myCode.length(), "100");
        }
        return myCode.toString();
    }

    @Transactional(readOnly = true)
    public List<ItemDto> getFrequentItems(String championName, String position) {
        List<ItemDto> list = championItemRepository.findAllByChampionNameAndPosition(championName, position)
                .get()
                .stream()
                .map((i) -> ItemDto.builder()
                        .name(i.getItemName())
                        .englishName(i.getEnglishName())
                        .src(i.getSrc())
                        .skillAccel(i.getSkillAccel())
                        .build())
                .collect(Collectors.toList());
        return list;
    }


}
