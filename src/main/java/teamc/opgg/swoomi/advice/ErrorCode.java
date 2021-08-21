package teamc.opgg.swoomi.advice;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    UnDefinedError(-9999, "정의되지 않은 에러입니다."),
    SummonerNotFoundException(-1001, "소환사를 찾을 수 없습니다."),
    RoomNotFoundException(-1002, "방을 찾을 수 없습니다."),
    SummonerNotInGameException(-1003, "소환사가 현재 게임 중이 아닙니다."),
    QrCodeFailException(-1004, "QR code 생성을 실패했습니다.");

    private int code;
    private String msg;
}
