# 파르페 텍스트 배치

기존 이미지 배치와 같은 헥사고날 구조로 텍스트를 저장한다. 이미지 업로드 URL 발급 없이 JSON 요청만으로 배치할 수 있다.

## 텍스트 저장

`POST /api/v1/groups/{groupId}/parfaits/{parfaitId}/texts`

기존 API와 동일한 Bearer 인증이 필요하다. 해당 그룹의 참여자만 ACTIVE 파르페에 배치할 수 있다.

```json
{
  "text": "오늘도 행복한 하루!",
  "positionX": 120.5,
  "positionY": 340.2,
  "positionZ": 2,
  "rotation": -30.0,
  "color": "#FF5733",
  "scale": 1.5,
  "fontSize": 16.0,
  "isFill": true,
  "isStroke": false,
  "textAlign": "CENTER"
}
```

`isFill`은 텍스트 내부 채움 여부, `isStroke`는 외곽선 표시 여부를 나타내는 Boolean이다. 각각 독립적으로 저장하며 둘 다 true 또는 둘 다 false인 값도 허용한다. `textAlign`은 텍스트 정렬로 `LEFT`, `CENTER`, `RIGHT` 중 하나다. 생략 시 기본값은 `isFill=false`, `isStroke=false`, `textAlign=CENTER`이며 나머지 필드는 필수다. 허용하지 않는 `textAlign` 값은 400 `INVALID_REQUEST`로 거절한다.

위치·각도는 이미지 배치와 같은 좌표계를 사용한다. `positionZ`는 이미지와 텍스트 사이의 겹침 순서를 표현한다. `fontSize`는 확대 전 기준 폰트 크기이고 `scale`은 그 크기에 적용하는 배율이다. 위 예시의 표시 크기는 `16 × 1.5 = 24`다. 두 값은 각각 저장·반환하며 서버에서 합치지 않는다.

텍스트는 공백만으로 구성할 수 없고 최대 2000자이며 줄바꿈을 보존한다. 색상은 기존 이미지 테두리 색상처럼 최대 20자의 문자열로 저장한다. `fontSize`와 `scale`은 유한한 양수, 좌표와 각도는 유한한 숫자여야 한다.

성공하면 201과 기존 `ApiResponse` 형식으로 `parfaitTextId`, 요청한 텍스트·스타일(`isFill`, `isStroke`, `textAlign` 포함), `placedBy`, `createdAt`을 반환한다. 같은 텍스트를 여러 번 요청해도 각각 독립된 배치로 저장한다.

## 텍스트 수정

`PATCH /api/v1/groups/{groupId}/parfaits/{parfaitId}/texts/{parfaitTextId}`

이미지 수정과 동일하게 작성자 본인만 ACTIVE 파르페의 텍스트 스타일을 수정할 수 있다. `text`는 수정 요청에서 제거되어 저장 후 내용을 변경할 수 없다. 요청에 `text`를 넣어도 무시된다. 위치·각도·색상·배율·폰트 크기·`isFill`·`isStroke`·`textAlign`을 부분 수정할 수 있다. 생략하거나 `null`로 보낸 필드는 기존 값을 유지하며, false를 전달하면 실제로 false로 변경한다. 스타일은 저장 시와 동일하게 검증한다.

```json
{
  "positionX": 0.0,
  "color": "#FFFFFF",
  "scale": 2.0,
  "isFill": false,
  "isStroke": true,
  "textAlign": "LEFT"
}
```

성공하면 200과 기존 `ApiResponse` 형식으로 다음 `data`를 반환한다. 응답의 `text`는 저장된 기존 내용이다. 배치자와 생성 시각도 유지한다.

```json
{
  "parfaitTextId": 301,
  "text": "오늘도 행복한 하루!",
  "positionX": 0.0,
  "positionY": 340.2,
  "positionZ": 2,
  "scale": 2.0,
  "rotation": -30.0,
  "color": "#FFFFFF",
  "fontSize": 16.0,
  "isFill": false,
  "isStroke": true,
  "textAlign": "LEFT"
}
```

## 텍스트 삭제

`DELETE /api/v1/groups/{groupId}/parfaits/{parfaitId}/texts/{parfaitTextId}`

작성자 본인만 ACTIVE 파르페의 텍스트를 삭제할 수 있다. 요청 본문은 필요 없다. 이미지 삭제와 동일하게 성공 시 200과 다음 응답을 반환한다.

```json
{
  "success": true,
  "code": "OK",
  "message": "요청이 성공적으로 처리되었습니다",
  "data": null,
  "errorDetail": null
}
```

수정·삭제 시 존재하지 않거나 경로의 파르페와 다른 텍스트는 404 `PARFAIT_TEXT_NOT_FOUND`, 작성자가 아니거나 그룹에서 나간 회원은 403 `PARFAIT_TEXT_NOT_OWNED`로 응답한다. 파르페가 해당 그룹에 없으면 404 `PARFAIT_NOT_FOUND`, 마감됐으면 409 `PARFAIT_ALREADY_CLOSED`로 응답한다. 수정 스타일이 잘못됐으면 400 `INVALID_TEXT_STYLE`로 응답한다.

## 파르페 조회

다음 기존 조회 API의 응답에 `texts`가 추가된다.

- `GET /api/v1/groups/{groupId}/parfaits/today`
- `GET /api/v2/groups/{groupId}/parfaits/today`
- `GET /api/v1/groups/{groupId}/parfaits/{parfaitId}`

`images`와 `texts`를 함께 사용하여 캔버스를 그린다. `texts`의 각 원소는 저장 응답과 같은 필드를 포함한다. `placedBy`는 이미지와 동일하게 `groupMemberId`, `nickname`, `nameTagChip`, `ownerType`을 포함한다. 텍스트가 없으면 기존 `images` 규칙과 동일하게 `texts: null`을 반환한다.

오늘 조회(v1·v2)와 상세 조회 모두 `data.texts[]`에 `isFill`, `isStroke`, `textAlign`을 반환한다. 일괄 위치 수정(`PATCH .../images`)은 `textAlign`을 바꾸지 않고 응답 `texts[]`에 현재 값만 담는다. 응답 원소 예시는 다음과 같다.

```json
{
  "parfaitTextId": 301,
  "text": "오늘도 행복한 하루!",
  "positionX": 0.0,
  "positionY": 340.2,
  "positionZ": 2,
  "scale": 2.0,
  "rotation": -30.0,
  "color": "#FFFFFF",
  "fontSize": 16.0,
  "isFill": false,
  "isStroke": true,
  "textAlign": "LEFT",
  "placedBy": {
    "groupMemberId": 10,
    "nickname": "연경이",
    "nameTagChip": "TYPE6",
    "ownerType": "ME"
  },
  "createdAt": "2026-10-20T14:30:00"
}
```

텍스트 저장·수정·삭제는 파르페 버전을 올리므로 v2 조회의 ETag가 갱신된다. 텍스트만 있는 파르페도 마감 시 CLOSED로 남는다. 과거 목록의 `imageCount`는 기존 의미대로 이미지 수만 센다.

## DDL

`persistence/src/main/resources/db/migration/V21__create_parfait_text.sql`에 `parfait_text` 테이블 생성 DDL이 있다. 배포 시 기존 Flyway 설정으로 적용된다. 파르페·배치자 ID와 파르페 조회 인덱스를 포함한다. `V23__add_text_align_to_parfait_text.sql`이 `text_align` 컬럼을 추가하며 기존 행은 `CENTER`로 채운다.

채움·외곽선 컬럼 추가 DDL은 `persistence/src/main/resources/db/migration/V22__add_fill_and_stroke_to_parfait_text.sql`이다. 기존 V21은 수정하지 않으며, 기존 행에는 `is_fill=0`, `is_stroke=0`이 적용된다.

```sql
ALTER TABLE parfait_text
    ADD COLUMN is_fill BIT(1) NOT NULL DEFAULT b'0' AFTER font_size,
    ADD COLUMN is_stroke BIT(1) NOT NULL DEFAULT b'0' AFTER is_fill;
```
