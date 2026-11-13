# Settings UI

LibrePods의 설정 화면에서 사용할 독립 Compose 라이브러리입니다. 모듈 이름과 공개 API는 기능에 따라 `Settings*`로 명명합니다. 원본 출처와 대조 범위는 [컴포넌트 출처 문서](../../docs/android-settings-components.md)에 기록합니다.

**`:app`의 기기 설정 화면이 이 모듈을 사용합니다.** `:app`은 `implementation(project(":settings-ui"))`로 의존하고, `BudsDeviceSettings`가 이 모듈의 컴포넌트와 `SettingsTheme`을 사용합니다. 이 모듈만 추가해서는 제품 디자인이 바뀌지 않으며, 화면 구성과 상태·명령은 여전히 `:app`이 소유합니다.

## 구성과 범위

| 용도 | 공개 API |
| --- | --- |
| 색상·글꼴·간격·모서리·창 그림자 | `SettingsTheme`, `SettingsStyle`, `SettingsGroupShape`, `SettingsWindowTheme`, `SettingsWindowOverlay` |
| 화면·헤더·스크롤 페이드 | `SettingsScaffold`, `SettingsScrollReporter` |
| 카드·섹션·구분선 | `SettingsGroupColumn`, `SettingsCard`, `SettingsList`, `SettingsSectionLabel`, `SettingsRowDivider` |
| 일반 행·선택 행·토글 행 | `SettingsListItem`, `SettingsChoiceRow`, `SettingsChoiceRowStyle`, `SettingsMultiChoiceRow`, `SettingsToggle` |
| 독립 스위치 | `SettingsSwitch` |
| 가로·세로 슬라이더 | `SettingsSeekBar`, `SettingsSeekBarRow`, `SettingsSeekBarStyle`, `SettingsVerticalSeekBar` |
| 일반·아이콘 버튼 | `SettingsButton`, `SettingsIconButton` |
| 아이콘 동작의 설명 말풍선 | `SettingsTooltip` |
| 이름 등의 텍스트 입력 | `SettingsInputField` |
| 대화상자·확인창·시트 | `SettingsDialog`, `SettingsDialogPresentation`, `SettingsConfirmationDialog`, `SettingsBottomSheet`, `SettingsSheetSurface`, `SettingsSheetTitle`, `SettingsSheetBody` |
| 듣기 모드·배터리 표시 | `SettingsModeStrip`, `SettingsBatteryRow`, `SettingsBatteryMeter` |
| 기기 화면의 이미지 배경·그림 | `SettingsImageBackdrop`, `SettingsDeviceIllustration`, `SettingsScaffold.background` |
| 저음·중음·고음 EQ | `SettingsEqualizer`, `SettingsEqualizerBand` |
| 작업 대기 표시 | `SettingsLoadingIndicator` |

대상은 LibrePods의 기존 설정 UI입니다. 로딩 표시는 기존 AirPods 상태 준비·진단 로그 읽기에, 하단 시트는 문의 작성·로그 내용 보기에 대응합니다. 원본의 업데이트 화면은 공통 요소를 관찰할 경로로 조사했지만, 업데이트 절차는 컴포넌트에 포함하지 않습니다. 업데이트가 뜨는 기기나 상태를 사용자가 준비할 필요도 없습니다. 실제 설정 경로의 증거와 허용된 별도 원본 렌더러 fixture의 증거를 구분하며, 대조하지 못한 상태는 미검증으로 기록합니다. 별도의 삼성 전용 기능, 프리셋 관리, 9대역 EQ 기능, 팝업 메뉴 체계, 진행률 막대, 기기 연결 엔진은 추가하지 않습니다. EQ는 원본의 세로 조절기 표현을 재사용하되, LibrePods에서 사용할 3개 대역을 호출자가 공급합니다.

## 스크린샷 기준 범위

현재 제품의16개 기기 설정 페이지와 iOS 자료는 [설정 참고 문서](../../docs/android-settings.md)에 정리합니다. 현재 원본은 Manager9.0.10.1724이며, 과거1704 픽셀 검증을 현재 결과로 확대하지 않습니다. 상태·명령 지원과 UI 표현의 검증은 구분합니다.

## 모듈 경계

`SettingsScaffold(scrollState=...)`는 콘텐츠 종류와 관계없이 측정한 헤더 공간을 한 번 예약합니다. `SettingsGroupColumn`을 사용하면 열의 `spacing` 값(기본 20dp) 하나가 실제 그룹 간격과 헤더 분할 반올림을 함께 결정합니다. 바깥 열은 이미 예약된 공간과의 반올림 차이만 적용하고, 중첩 열은 자신의 그룹만 배치합니다. Scaffold에는 별도의 그룹 간격 값이 없습니다. 섹션의 16dp 선행 공간과 배터리의 14dp 후행 공간 예외는 유지됩니다. 내부 간격을 따로 가진 콘텐츠를 감싸는 무간격 바깥 열은 `spacing = 0.dp`로 표현합니다.

```text
소비 앱: 상태·지원 여부·문구·그림·내비게이션·기기 명령
              ↓ 값 / 콜백 / Compose 슬롯
:settings-ui: theme / components
```

- `:app`에 의존하지 않습니다. ViewModel, 서비스, Bluetooth, 앱 preferences, 모델 enum, 앱 `R`을 참조하지 않습니다. 페이드가 겹치는 내비게이션 영역을 판단할 때 시스템의 내비게이션 방식·태스크바 설정을 읽으며, 이를 변경하거나 저장하지 않습니다.
- 공개 컴포넌트가 기기 명령을 보내거나 값을 저장하지 않습니다. 변경 요청은 콜백으로 돌려줍니다. 대화상자 확인·취소·시트 닫기도 호출자가 상태를 갱신합니다.
- 배터리의 좌우 통합, 유효성, 충전 상태 및 현지화된 값은 호출자가 계산합니다. `SettingsBatteryRow`에는 통합 또는 분리 표시할 `primaryMeters`와 선택적인 `secondaryMeter` 슬롯을 전달합니다. 280dp 이내의 행에서 기본 그룹과 보조 슬롯의 폭을 나누고, 3dp 간격과 상단 정렬을 유지합니다. `SettingsBatteryMeter`의 `maxWidth`가 개별 슬롯 폭을 제한하며, 링과 라벨은 그 폭의 가운데에 놓입니다. 복합 이름과 인라인 충전 그림은 선택적인 `nameContent` 슬롯으로 공급합니다. 기본 진행 색은 원본의 10% 이하 저전력 색과 나머지 정상 색을 따르며 `progressColor`로 지정할 수 있습니다. 표시 여부나 저전력 알림은 호출자가 정합니다. 모드의 식별자·문구·아이콘은 슬롯으로 공급합니다.
- 스크롤 소유권, 헤더 애니메이션과 스위치 장식 렌더러는 내부 구현입니다. 행 스위치는 행 하나에 접근성 동작을 부여하고, 독립 스위치는 별도 접근성 동작을 제공합니다.
- API 33 이상을 대상으로 합니다. 글꼴은 기기의 선택적 `sec` 패밀리를 사용하며 없는 기기에서는 시스템 글꼴로 대체됩니다. 삼성 APK·그림·폰트·추출 바이너리를 포함하지 않습니다. 저장소의 GPL 라이선스를 따릅니다.

## 배터리 표시 구성

`SettingsBatteryRow`는 호출자가 선택한 하나 또는 두 개의 기본 미터와 선택적인 케이스 미터를 배치합니다. 미터 내부의 이름·값·접근성 설명·그림은 호출자가 공급합니다.

```kotlin
val primaryMeters: List<@Composable () -> Unit> = listOf(
    {
        SettingsBatteryMeter(
            level = leftLevel, name = leftLabel, value = leftValue,
            description = leftDescription, maxWidth = 138.5.dp,
            artwork = leftArtwork,
        )
    },
    {
        SettingsBatteryMeter(
            level = rightLevel, name = rightLabel, value = rightValue,
            description = rightDescription, maxWidth = 138.5.dp,
            artwork = rightArtwork,
        )
    },
)
SettingsBatteryRow(primaryMeters = primaryMeters, secondaryMeter = caseMeter)
```

## 사용 예

통합할 때 소비 모듈에 `implementation(project(":settings-ui"))`를 추가하고 `me.kavishdevar.librepods.ui.theme`와 `.components`를 가져옵니다. 현재 `:app`은 이 모듈에 의존합니다.

```kotlin
SettingsTheme {
    val scroll = rememberScrollState()
    SettingsScaffold(title = title, backLabel = backLabel, scrollState = scroll) {
        SettingsList(title = sectionTitle) {
            SettingsToggle(
                label = label,
                checked = state.enabled,
                enabled = state.available,
                onCheckedChange = onEnabledChange,
            )
            SettingsChoiceRow(
                name = choiceLabel,
                selected = state.selected,
                onClick = onChoiceSelected,
            )
        }
    }
}
```

스크롤 화면은 호출자가 기억하는 `ScrollState`를 `SettingsScaffold`에 전달합니다. Scaffold가 전체 화면의 스크롤 영역과 내부 헤더·시스템 인셋·20dp 끝 여백을 배치하므로, 본문에 별도의 `verticalScroll`을 붙이지 않습니다. 본문이 헤더 아래로 올라가면 제목을 숨기고 뒤로가기·액션을 떠 있는 영역에 표시합니다. 호출자의 `Modifier`는 scaffold 전체에 적용됩니다.

기존 방식으로 호출자가 스크롤을 소유하는 화면은 `scrollState` 없이 같은 scaffold 안에서 `SettingsScrollReporter(scroll.canScrollForward)`를 호출할 수 있습니다. 이 방식은 고정 헤더 아래의 본문 영역을 사용하며 떠 있는 헤더 동작을 제공하지 않습니다. 고정 화면은 스크롤 상태를 전달하거나 보고하지 않습니다.

일반 메뉴 행과 `Menu` 선택 행은 내부 `settingsRowFeedback`을 공유합니다. 누르면 내용만 중심 기준 .98배로 줄고, 행 크기의 사각형 배경 피드백은 크기를 유지합니다. 상위 카드가 모서리를 자릅니다. 원본의 100ms 진입·350ms 복원 곡선과 별도 행 색상(다크 `#29FFFFFF`, 라이트 `#1A000000`)을 사용합니다. 클릭·선택·활성 상태·별도 스위치 동작은 계속 호출자가 소유합니다.

일반·상단 토글도 같은 행 피드백을 공유합니다. 상단 토글은 다크 `#33FFFFFF` 색상을 사용합니다. 스위치 영역을 누르면 행 효과 대신 스위치 본체와 피드백을 함께 .96배로 줄이고, 그 안의 손잡이 내용은 .8배로 줄입니다. 손잡이의 원형 피드백에는 5.5dp 여백을 더합니다. 스위치 영역은 별도의 포인터 경로를 쓰며, 접근성·키보드 동작은 행의 `toggleable` 하나가 소유합니다. 누른 도중 비활성화하거나 제거하면 포인터 처리를 취소합니다. 값과 콜백은 호출자가 공급합니다.

`SettingsChoiceRow`의 기본 `Menu`는 일반 설정 선택 행입니다. 길게 누르기 등 제어 설정의 표현은 `style = SettingsChoiceRowStyle.Control`로 선택합니다. `Control`은 행 전체의 위아래 14dp 여백, 라디오 뒤 14dp 간격, 아이콘 뒤 16dp 간격과 62dp 구분선 시작 여백을 사용합니다. 눌림은 공통 버튼 피드백을 재사용해 내용과 테마 on-surface 색의 매끄러운 pill 배경을 함께 .96배로 줄입니다. 비활성 글자·그림은 .4로 표시하고, 라디오는 원본처럼 비활성 색에 별도의 .4 레이어를 더합니다. 아이콘과 설명 문구는 호출자가 공급합니다. 원본 아이콘 크기는 `leadingContent` 안에서 `Modifier.size(28.dp)`로 지정합니다. 빈 설명 문자열도 원본처럼 한 줄을 차지합니다.

`Control` 행에 앱 아이콘 바탕이 필요하면 `leadingIconBackdrop = true`를 지정하고 `leadingContent`에 그림을 공급합니다. 이 옵션은 28dp 슬롯에 11dp 원형 모서리, 0.1dp 테두리와 0.8dp 안쪽 여백을 적용합니다. 비활성 상태에서도 흰 바탕을 유지하고 그림만 흐리게 표시합니다. `Control` 스타일과 비어 있지 않은 `leadingContent`가 필요합니다.

선택 행의 `trailingAction`은 행과 별도의 클릭·접근성 동작을 유지합니다. 28dp 장식 슬롯 안에서도 동작 버튼의 48dp 영역과 아이콘 크기를 압축하지 않습니다. 행이 비활성인 상태에서도 별도 동작은 호출자가 공급한 활성 상태를 따릅니다.

메뉴 이동과 켜기·끄기가 분리된 행은 `SettingsListItem.trailingContent`에 `SettingsSwitch(compact = true)`를 넣습니다. 이 옵션은 스위치 그림의 자연 크기로 배치하고 Compose의 확장 터치 영역을 유지합니다. 기본 독립 스위치는 48dp 공간을 확보합니다. 행 스위치와 본체·손잡이 피드백 렌더러를 공유하되, 독립 스위치의 입력·키보드·이름 있는 접근성 동작은 자체 `toggleable`이 소유합니다. 일반 토글 행은 `SettingsToggle`로 하나의 동작을 제공합니다. 메뉴 행을 비활성화하면 아이콘·글자·뒤쪽 콘텐츠 전체에 불투명도 .4를 적용하며, 별도 스위치의 동작 가능 여부는 그 스위치의 `enabled` 값이 결정합니다.

`SettingsInputField`에는 호출자가 기억하는 `TextFieldState`와 필요할 때 `FocusRequester`를 전달합니다. 플랫폼 EditText가 입력·IME·선택을 처리하고, 호출자 상태와 글자·선택 범위를 양방향으로 동기화합니다. 원본에서 확인한 공개 Material.Light 입력 위젯, Sans 글꼴, 정수 리소스 크기와 입력선 패딩을 사용합니다. API 35 이상에서는 locale 최소 줄 높이를 적용하며, 기본 커서는 시스템의 동적 accent 600 색이며, 선택 배경은 같은 색의 40%입니다. `cursorColor`와 `selectionColor`로 각각 지정할 수 있습니다. 입력 길이와 명령 조건은 소비 앱에서 검사하고 `errorMessage`를 전달합니다. 오류는 접근성 노드에도 공급하며 플랫폼 오류 팝업은 만들지 않습니다. `KeyboardType.Password`·`NumberPassword`는 한 줄·여러 줄과 호출자 값 갱신에서도 마스킹을 유지합니다.

그림자는 Android 창 테마의 광원과 명암 값을 사용합니다. 소비 앱의 창 테마가 `SettingsWindowTheme`를 상속하면 원본의 공개 Android 부모인 `Theme.DeviceDefault.Light.NoActionBar`와 `Theme.DeviceDefault.NoActionBar`를 night 설정에 따라 사용합니다. 밝은 테마의 ambient/spot 명암은 .04/.1, 어두운 테마는 .1/.35입니다. 뷰를 만들기 전에 창 테마를 지정하고, 창의 리소스 night 설정과 `SettingsTheme(darkTheme)`를 맞춰야 합니다. Compose 테마는 창을 자동으로 변경하지 않습니다.

스크롤로 가려진 헤더는 높이만 유지하고 입력 노드를 제거합니다. 따라서 떠 있는 버튼이 잠시 숨겨진 동안에도 본문 터치를 가로채지 않습니다.

아이콘 버튼을 누르면 그림만 .96배로 줄고 12dp 여백의 원형 표시는 크기를 유지합니다. 길게 누르면 호출자가 공급한 `contentDescription`이 말풍선에 표시되며 클릭 동작은 실행하지 않습니다. 비활성화하거나 버튼을 제거하면 말풍선을 닫습니다. 공개 버튼의 기본 터치 영역은 48dp이며, 작은 헤더 안에서는 원본처럼 고정 48dp 슬롯 가운데 24dp 조작부를 둡니다. 두 치수를 따로 반올림하므로 450dpi에서도 원본 경계가 유지됩니다. 호출자의 정렬·가중치는 바깥 레이아웃에 적용하고, 클릭과 길게 누르기는 이름 있는 하나의 접근성 버튼에 연결합니다. 키보드 포커스로도 말풍선을 표시하며 Escape로 닫습니다.

`SettingsTooltip(text)`는 별도로 재사용할 수 있는 말풍선 표면입니다. 16sp 일반 굵기·가운데 정렬, 18/10dp 내용 여백, 8dp 그림자와 부드러운 알약 모서리를 사용하며 글자 배율은 1.3으로 제한합니다. 아이콘에 연결된 팝업은 그림 아래 6dp에 우선 배치하고, 아래 공간이 부족하면 위로 옮깁니다. 창 가장자리 제한은 좌우 10dp·아래 50dp입니다. 터치·호버와 팝업 수명은 Compose `TooltipBox`가 처리하며, 공유된 버튼 포커스와 Escape 입력을 연결합니다. 다른 글자·확대 글꼴·호버·키보드 포커스·전환 시간과 색을 지정한 외곽 버튼은 원본 대조 범위에 포함하지 않습니다.

호출자의 정렬·가중치와 전체 클릭 영역, 단일 버튼의 클릭·도움말 동작, 키보드 실행·도움말 닫기, 길게 누르기의 클릭 억제와 말풍선 정리를 포함한 실기 테스트 7개는 통과했습니다.

`SettingsVerticalSeekBar`에는 제한된 높이와 현지화한 `label`을 공급합니다. `SettingsEqualizerBand.contentDescription`은 해당 조절기의 범위·변경 동작과 같은 접근성 노드에 붙습니다. 가로 Standard 조절기와 내부 손잡이 피드백을 공유해 내용만 .98배로 줄이고 6dp 여백의 원형 표시는 크기를 유지합니다. 입력·호버는 실제 `VerticalSlider`와 그 손잡이 슬롯에 연결하고, 별도로 그리는 손잡이는 입력을 받지 않습니다.

호출자가 드래그 중 외부 값 갱신을 조정해야 하면 `interactionSource`에 기억한 `MutableInteractionSource`를 전달하고 `collectIsDraggedAsState()`로 관찰합니다. 기존 빠른 설정의 음량 조절기가 사용하는 드래그 상태도 이 경로로 유지할 수 있습니다. `onValueChangeFinished`는 Compose 조절기의 완료 콜백을 전달하며, 비활성 상태에서는 값·완료 콜백을 차단합니다. 값을 저장하거나 음량 명령을 보내는 동작은 호출자가 담당합니다. 실제 통합은 아직 하지 않았습니다.

이 결과는 기록한 그래프 카드에 한정됩니다. LibrePods의 3대역·0..100 구성은 호출자 적용이며, 프리셋 전체 화면·기록한 프로필 밖의 값·기록한 상태 밖의 드래그 표시·비활성·호버·포커스·확대 글꼴·전환 프레임은 아직 대조하지 않았습니다. [EQ 검증 기록](../../docs/android-settings-components.md)에 원본 버전, 합성 입력 범위, fixture 빌드·PNG·독립 검산, 제외한 시스템 전환 캡처와 화면 복원 기록을 연결했습니다.

슬라이더 값은 유한한 수이고 범위 안에 있어야 하며, 범위의 끝은 시작보다 커야 합니다. `SettingsEqualizer`의 기본 `valueRange = -10..10`과 부호 표기는 원본 대조용입니다. LibrePods EQ는 `valueRange = 0..100`, `valueLabel = { it.toString() }`를 전달해 기존 101단계를 그대로 사용합니다. `valueLabel`은 현지화한 표시 문구만 만들며 값이나 명령을 변환하지 않습니다. 정수 간격은 슬라이더의 `steps`에도 전달해 방향키와 Android 접근성 증감이 한 단계씩 움직이게 합니다. 대역 값 갱신·디바운스·기기 명령은 계속 호출자가 담당합니다. S25 검사에서 50→51 한 단계 변경과 0·100 끝값, 호출자 값 갱신 시 콜백이 발생하지 않는 동작을 확인했습니다. 모드 strip은 기존 화면의 3~4개 항목 배치를 대상으로 하며 항목마다 같은 슬롯 순서를 유지합니다. 아이콘과 문구는 하나의 선택 영역과 접근성 동작을 공유합니다. `artwork`는 장식용 슬롯이며, 모드의 접근성 이름은 `label`이 공급합니다.

여러 항목을 각각 켜고 끄는 목록에는 `SettingsMultiChoiceRow(label, checked, onCheckedChange)`를 사용합니다. 길게 누를 때 전환할 소음 제어 모드처럼 독립된 선택에 대응하며, 값을 저장하거나 다른 항목의 선택을 해제하지 않습니다. 원본의 32dp 원형 선택 표시, 16/18dp 좌우·12dp 위아래 여백, 14dp 그림 뒤 간격과 기본 시스템 글꼴 14sp를 사용합니다. 눌림은 공통 행 피드백을 재사용해 내용만 .98배로 줄이고, 테마 on-surface 색의 매끄러운 pill 배경은 크기를 유지합니다. 원본의 중복 라디오 동작을 복제하지 않고, 이름과 켜짐·꺼짐을 갖는 단일 체크박스 접근성 동작을 제공합니다. 비활성 상태에서는 변경 콜백을 호출하지 않습니다.

`SettingsBottomSheet`는 이 표면에 10dp 외곽 여백을 주는 Compose 모달 어댑터입니다. 원본처럼 확장 상태로 열고 바깥 터치로 닫지 않는 것이 기본입니다. 배경은 창의 `DIM_BEHIND`로 어둡게 하며, 그 위에 검은 scrim을 그리지 않습니다. 원본의 8dp 창 고도를 카드의 8dp 뷰 고도와 별도로 적용해 블러 샘플링의 창 내부 여백까지 맞춥니다. DIM 값은 DeviceDefault 플랫폼 창 정책을 유지하므로 현재 S25에서는 다크 0.65, 라이트 기본 0.18, 라이트 투명도 줄이기 0.35 정책을 사용합니다. 0.35는 현재 프레임워크 소스에서 확인했으며 기기 설정은 변경하지 않았습니다. 플랫폼이 DIM을 초기화하지 않은 경우에는 호출자 창 테마의 값을 공급합니다. 제거 시 추가한 DIM 플래그·고도와 직접 초기화한 DIM 값만 복원합니다. 소비 앱의 창 테마와 `SettingsTheme`의 밝기 설정을 맞춰야 합니다. 기존 연락처 폼의 부분 확장·바깥 터치 닫힘이 필요하면 각각 `allowPartialExpansion = true`, `dismissOnClickOutside = true`로 지정합니다. 표시 여부·내용·동작·닫힘 상태는 호출자가 소유합니다. 실제 설정 경로의 시트와 부분 확장·드래그·전환 프레임은 아직 대조하지 않았습니다.

## 확인과 유지보수

`src/debug/.../catalog/SettingsComponentGallery.kt`에 light/dark/확대 글꼴 Preview와 실제 콜백을 사용하는 예제가 있습니다. 그림은 단순한 자체 도형이며 제품 그림을 포함하지 않습니다. Preview는 출시 AAR에 포함되지 않습니다.

저장소 루트에서:

```sh
./android/gradlew -p android :settings-ui:assembleDebug :settings-ui:testDebugUnitTest \
  :settings-ui:assembleDebugAndroidTest --console=plain --no-daemon --max-workers=2 \
  '-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8'
```

JDK/SDK 설정은 [Android 개발 가이드](../../docs/android-development.md)를 따릅니다. 호스트 테스트는 화면 교체 시 페이드 보고자의 소유권을 검증합니다. 계측 테스트는 입력 포커스의 획득·해제·재요청, 글자·선택의 양방향 동기화와 오류 접근성, 좁은 입력창의 명명된 동작·호출자 검증 상태와 닫은 뒤 재개방·편집·취소, 행 스위치의 단일 동작과 비활성화, 메뉴 이동과 compact 스위치의 독립 동작·활성 상태, 비활성 선택 행 안의 독립 동작과 아이콘 크기, 다중 선택의 독립 상태·단일 접근성 동작·비활성 콜백 차단, 확인/취소 콜백 분리, 긴 대화상자 본문의 스크롤과 고정 제목·버튼, 긴 버튼 문구의 세로 배치와 비활성 콜백 차단, 가로 조절 범위의 현지화된 이름과 비활성 콜백 차단, 세로 슬라이더 방향·범위, 로딩 표시의 단일 접근성 범위와 제거 시 애니메이션 중단, 긴 시트의 동작 버튼 스크롤과 호출자 소유 닫힘·뷰 제거, 시트의 바깥 터치 유지와 다크 모드 글자색, 제한된 화면에서 스크롤 후 하나의 뒤로가기 동작에 접근할 수 있는지를 검증합니다. 계측 APK는 제품 APK와 다른 `me.kavishdevar.librepods.ui.test` 패키지입니다.
