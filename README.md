# AI mini锛圓ndroid / WebView 鍐呮牳锛?

鍩轰簬绯荤粺 `android.webkit.WebView` 鐨?AI mini 瀹㈡埛绔€傚姛鑳藉榻?GeckoView 姝ｅ紡鐗堬紙杩炴帴绠＄悊銆佷笅杞姐€佷换鍔￠€氱煡銆佹闈㈡ā寮忋€佺晫闈㈣缃€侀〉闈㈡ˉ鎺ョ瓑锛夛紝娴忚鍣ㄥ唴鏍镐繚鎸?WebView锛屼究浜庤交閲忓畨瑁呬笌绯荤粺鍐呮牳澶嶇敤銆?

> 鏈伐绋嬬敱 GPT Mini锛圵ebView 鐗?2.0.3锛夋紨杩涜€屾潵锛岀幇宸蹭綔涓?*鐙珛鐨勬柊搴旂敤**锛氬簲鐢ㄥ悕 `AI mini`銆佸寘鍚?`app.aimini`銆佷娇鐢ㄨ嚜宸辩殑鍙戝竷绛惧悕锛屽彲涓庢棫鐗?GPT Mini 骞跺瓨瀹夎銆?

褰撳墠鐗堟湰锛?

- 搴旂敤鍚嶏細`AI mini`
- versionName锛歚1.0.3`
- versionCode锛歚4`
- 浠撳簱锛?https://github.com/3354282339/AI-mini>
- 涓嬭浇瀹夎鍖咃細<https://github.com/3354282339/AI-mini/releases/latest>
- applicationId锛歚app.aimini`锛堜笌鏃х増 `app.gptmini.webview`銆丟ecko 鐗?`app.gptmini` 浜掍笉鍐茬獊锛屽彲鍚屾椂瀹夎锛?
- 鏈€浣庣郴缁燂細Android 8.0锛圓PI 26锛?
- 娴忚鍣ㄥ唴鏍革細绯荤粺 WebView

## 鏋勫缓

Windows锛堟湰鏈?JDK / Android SDK / Gradle 宸查厤濂斤紝璇﹁ `..\android-env.md`锛夛細

```powershell
cd D:\aicoding\AImini\GPT-Mini-Android
.\gradlew.bat assembleDebug      # 璋冭瘯鍖?
.\gradlew.bat assembleRelease    # 姝ｅ紡鍖咃紙闇€瑕佺鍚嶉厤缃紝瑙佷笅锛?
```

鍏朵粬鐜锛?

```bash
chmod +x scripts/build-debug.sh
./scripts/build-debug.sh
```

APK 杈撳嚭锛?

```text
app/build/outputs/apk/debug/AI mini-1.0.1-debug.apk
app/build/outputs/apk/release/AI mini-1.0.1-release.apk
```

## 绛惧悕

`keystore/ai-mini-release.jks`锛堝埆鍚?`aimini`锛? `keystore/ai-mini-signing.properties` 鏄?AI mini 鑷繁鐨勫彂甯冭瘉涔︼紝涓よ€呴兘鍦?`.gitignore` 閲岋紝**涓嶈繘浠撳簱锛屽繀椤诲崟鐙浠?*锛涗涪浜嗚繖涓瘉涔﹀氨鏃犳硶鍐嶇粰宸插畨瑁呯敤鎴峰仛瑕嗙洊鍗囩骇銆?

绛惧悕鎸囩汗锛圫HA-256锛夛細

```text
42A656FA6D22B8FE11C698EBEADB42CB85A6FAC4BC8FBBA5834EB8CAE5B718CF
```

## 澶氳澶?/ 澶氳繛鎺?

AI mini 鎸夈€?*涓€涓綉鍧€ = 涓€鍙拌澶?*銆嶇鐞嗚繛鎺ワ紝姣忓彴璁惧甯︿竴涓?*绫诲瀷**锛屼负鍚庣画鎺ュ叆涓嶅悓浜у搧锛坺code銆乨sh 鈥︼級鐣欏ソ浣嶇疆锛?

| 绫诲瀷 | 鏍囪瘑 | 璇存槑 |
| --- | --- | --- |
| 鏈湴 WebUI | `miniapp` | 褰撳墠杩欏 Codex / GPT Mini WebUI锛屾敮鎸佷换鍔＄姸鎬侀€氱煡 |
| zcode | `zcode` | 鍙︿竴涓?WebUI锛屾寜缃戝潃鎵撳紑鐣岄潰 |
| dsh | `dsh` | 鍚屼笂 |
| 鍏跺畠 | `other` | 浠绘剰 HTTP(S) 缃戦〉 |

- 棣栭〉銆?*鎴戠殑璁惧**銆嶅垪鍑哄凡淇濆瓨璁惧锛堟渶澶?4 鍙帮紝鏇村鍙偣銆屾煡鐪嬪叏閮ㄣ€嶏級锛岀偣鍑诲嵆鍒囨崲锛屽綋鍓嶈澶囬珮浜樉绀恒€?
- 鎵嬪姩杈撳叆缃戝潃鏃跺彲鐢ㄣ€?*璁惧绫诲瀷**銆嶄笅鎷夋寚瀹氱被鍨嬶紱绫诲瀷涔熶細鎸?URL 鍏抽敭瀛楄嚜鍔ㄦ帹鏂紙鍚?`zcode` 鈫?zcode锛屽惈 `dsh`/`deepseek` 鈫?dsh锛屽惈 `codex`/`mini` 鈫?鏈湴 WebUI锛夛紝鍙殢鏃跺湪璁惧绠＄悊澶勬敼銆?
- 鎮诞鑿滃崟绗竴椤规槸銆?*鍒囨崲璁惧**銆嶏紝鍓爣棰樻樉绀?`绫诲瀷 路 璁惧鍚峘锛涚偣寮€灏辨槸璁惧鍒楄〃銆?
- 銆岀鐞嗚澶囥€嶉噷鍙噸鍛藉悕銆?*鏀圭被鍨?*銆佸垹闄わ紱鏀瑰畬绔嬪嵆鐢熸晥銆?
- 閫氱煡琛屼负鎸夌被鍨嬪垎鏀細鍙湁 `miniapp` 璧?`/codex/status` 浠诲姟鐩戞帶锛泎code / dsh / 鍏跺畠绫诲瀷淇濈暀甯搁┗閫氱煡浣嗕笉鍙戠姸鎬佽姹傘€?

## 鐩稿叧鏂囨。

- [AI mini 搴旂敤浠嬬粛涓庝娇鐢ㄥ府鍔(AI%20mini-搴旂敤浠嬬粛涓庝娇鐢ㄥ府鍔?md)
- [AI mini 寮€鍙戜氦鎺ヨ鏄嶿(AI-寮€鍙戜氦鎺ヨ鏄?md)
- [瀹炲喌绐椾笌閫氱煡閫傞厤璇存槑](瀹炲喌绐椾笌閫氱煡閫傞厤璇存槑.md)
