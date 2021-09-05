package teamc.opgg.swoomi.service;

import com.merakianalytics.orianna.Orianna;
import com.merakianalytics.orianna.types.common.Region;
import com.merakianalytics.orianna.types.core.searchable.SearchableList;
import com.merakianalytics.orianna.types.core.spectator.CurrentMatch;
import com.merakianalytics.orianna.types.core.spectator.Player;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import teamc.opgg.swoomi.advice.exception.CSummonerNotFoundException;
import teamc.opgg.swoomi.advice.exception.CSummonerNotInGameException;
import teamc.opgg.swoomi.dto.*;
import teamc.opgg.swoomi.entity.MatchTeamCodeSummoner;
import teamc.opgg.swoomi.repository.ChampionItemRepository;
import teamc.opgg.swoomi.repository.ItemPurchaseRepository;
import teamc.opgg.swoomi.repository.MatchTeamCodeSummonerRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
public class MatchService {

    @Autowired
    private ItemPurchaseRepository itemPurchaseRepository;
    @Autowired
    private ChampionItemRepository championItemRepository;
    @Autowired
    private MatchTeamCodeSummonerRepository matchTeamCodeSummonerRepository;

    @Autowired
    private ItemPurchaseService itemPurchaseService;

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
    public List<PlayerDto> getOpData(String data, boolean flag) {
        String summonerName = data;
        if (flag) {
           summonerName = matchTeamCodeSummonerRepository.findFirstByMatchTeamCode(data).get().getSummornerName();
        }
        Summoner summoner = Orianna.summonerNamed(summonerName).withRegion(Region.KOREA).get();
        if (!summoner.exists()) {
            log.info("NO SUMMONER NAMED : " + summonerName);
            throw new CSummonerNotFoundException();
        }
        if (!summoner.isInGame()) {
            log.info("SUMMONER '" + summonerName + "' NOT IN GAME");
            throw new CSummonerNotInGameException();
        }
        List<PlayerDto> playerDtos = new ArrayList<>();

        // 1. 상대팀 구하기
        SearchableList<Player> sList = summoner.getCurrentMatch().getParticipants();
        String finalSummonerName = summonerName;
        Player tempPlayer = sList.find((playerName) -> playerName.getSummoner().getName().equals(finalSummonerName));
        String teamId = tempPlayer.getTeam().toString();

        // 2. 상대팀 멤버 구하기
        SearchableList<Player> opList = sList.filter((player) -> !player.getTeam().toString().equals(teamId));

        for (Player p : opList) {
            String championName = p.getChampion().getName();
            List<ItemDto> list = championItemRepository.findAllByChampionName(championName)
                    .get()
                    .stream()
                    .map((e) -> ItemDto.builder()
                        .name(e.getItemName())
                        .englishName(e.getEnglishName())
                        .skillAccel(e.getSkillAccel())
                        .src(e.getSrc())
                        .build())
                    .collect(Collectors.toList());

            PlayerDto dto = PlayerDto.builder().summonerName(p.getSummoner().getName())
                    .championName(championName)
                    .championImgUrl(p.getChampion().getImage().getURL())
                    .ultImgUrl(p.getChampion().getSpells().get(3).getImage().getURL())
                    .spellDImgUrl(p.getSummonerSpellD().getImage().getURL())
                    .spellFImgUrl(p.getSummonerSpellF().getImage().getURL())
                    .frequentItems(list)
                    .build();

            playerDtos.add(dto);
        }
        return playerDtos;
    }

    @Transactional
    public MatchStatusDto getMatchTeamCode(String summonerName) {

        boolean isMyTeam = false;
        long myTeam = 100;
        Summoner summoner;
        MatchStatusDto matchStatusDto = MatchStatusDto.builder()
                .isStarted(false)
                .matchTeamCode("")
                .build();

        summoner = Orianna.summonerNamed(summonerName).withRegion(Region.KOREA).get();
        if (!summoner.exists()) {
            throw new CSummonerNotFoundException();
        }

        CurrentMatch currentMatch = Orianna.currentMatchForSummoner(summoner).get();
        if (currentMatch.getId() != 0) {
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

            MatchTeamCodeSummoner matchTeamCodeSummoner = MatchTeamCodeSummoner.builder()
                    .matchTeamCode(matchTeamCode)
                    .summornerName(summonerName)
                    .build();

            matchTeamCodeSummonerRepository.save(matchTeamCodeSummoner);
        }
        return matchStatusDto;
    }

//    public void postItemPurchase(ItemPurchaseDto body) {
//        List<ItemPurchase> list = body.convertToEntity();
//        itemPurchaseRepository.saveAll(list);
//    }

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
                .collect(Collectors.toList())
        ;
        return list;
    }


}
