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
  "fontSize": 16.0
}
```

모든 필드는 필수다. 위치·각도는 이미지 배치와 같은 좌표계를 사용한다. `positionZ`는 이미지와 텍스트 사이의 겹침 순서를 표현한다. `fontSize`는 확대 전 기준 폰트 크기이고 `scale`은 그 크기에 적용하는 배율이다. 위 예시의 표시 크기는 `16 × 1.5 = 24`다. 두 값은 각각 저장·반환하며 서버에서 합치지 않는다.

텍스트는 공백만으로 구성할 수 없고 최대 2000자이며 줄바꿈을 보존한다. 색상은 기존 이미지 테두리 색상처럼 최대 20자의 문자열로 저장한다. `fontSize`와 `scale`은 유한한 양수, 좌표와 각도는 유한한 숫자여야 한다.

성공하면 201과 기존 `ApiResponse` 형식으로 `parfaitTextId`, 요청한 텍스트·스타일, `placedBy`, `createdAt`을 반환한다. 같은 텍스트를 여러 번 요청해도 각각 독립된 배치로 저장한다.

## 파르페 조회

다음 기존 조회 API의 응답에 `texts`가 추가된다.

- `GET /api/v1/groups/{groupId}/parfaits/today`
- `GET /api/v2/groups/{groupId}/parfaits/today`
- `GET /api/v1/groups/{groupId}/parfaits/{parfaitId}`

`images`와 `texts`를 함께 사용하여 캔버스를 그린다. `texts`의 각 원소는 저장 응답과 같은 필드를 포함한다. `placedBy`는 이미지와 동일하게 `groupMemberId`, `nickname`, `nameTagChip`, `ownerType`을 포함한다. 텍스트가 없으면 기존 `images` 규칙과 동일하게 `texts: null`을 반환한다.

텍스트 저장도 파르페 버전을 올리므로 v2 조회의 ETag가 갱신된다. 텍스트만 있는 파르페도 마감 시 CLOSED로 남는다. 과거 목록의 `imageCount`는 기존 의미대로 이미지 수만 센다.

## DDL

`persistence/src/main/resources/db/migration/V20__create_parfait_text.sql`에 `parfait_text` 테이블 생성 DDL이 있다. 배포 시 기존 Flyway 설정으로 적용된다. 파르페·배치자 외래 키와 파르페 조회 인덱스를 포함한다.
