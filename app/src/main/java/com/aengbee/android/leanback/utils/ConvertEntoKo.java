package com.aengbee.android.leanback.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by i5 on 2017-04-06.
 */

public class ConvertEntoKo {




        private static final char[] CHO =
		/*ㄱ ㄲ ㄴ ㄷ ㄸ ㄹ ㅁ ㅂ ㅃ ㅅ ㅆ ㅇ ㅈ ㅉ ㅊ ㅋ ㅌ ㅍ ㅎ */
                {0x3131, 0x3132, 0x3134, 0x3137, 0x3138, 0x3139, 0x3141, 0x3142, 0x3143, 0x3145,
                        0x3146, 0x3147, 0x3148, 0x3149, 0x314a, 0x314b, 0x314c, 0x314d, 0x314e};

        private static final char[] JUN =
		/*ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ*/
                {0x314f, 0x3150, 0x3151, 0x3152, 0x3153, 0x3154, 0x3155, 0x3156, 0x3157, 0x3158,
                        0x3159, 0x315a, 0x315b, 0x315c, 0x315d, 0x315e, 0x315f, 0x3160,	0x3161,	0x3162,
                        0x3163};
        /*X ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ*/
        private static final char[] JON =
                {0x0000, 0x3131, 0x3132, 0x3133, 0x3134, 0x3135, 0x3136, 0x3137, 0x3139, 0x313a,
                        0x313b, 0x313c, 0x313d, 0x313e, 0x313f, 0x3140, 0x3141, 0x3142, 0x3144, 0x3145,
                        0x3146, 0x3147, 0x3148, 0x314a, 0x314b, 0x314c, 0x314d, 0x314e};

        public  static String concatOldNew(String Old, String New){
            String result = "";

            char FirstNew = New.charAt(0);
            if(Old != null && !Old.isEmpty()) {
                char LastOld = Old.charAt(Old.length()-1);
                if (New.equals("ic_backspace_white_48dp"))
                    result = Backspace(Old);
                else if(FirstNew >= 0x1100 && FirstNew <= 0x11FF){
                    if(LastOld >= 0x1100 && LastOld <= 0x11FF){
                        result = engToKor(kor2eng(Old+New));
                    }
                    else if(LastOld >= 0xAC00 && LastOld<= 0xD7A3 ) {
                        //Log.d("aaa", "concatOldNew: "+splitKor2Eng(Old) + "|"+kor2eng(New) +"|"+ engToKor(splitKor2Eng(Old) + kor2eng(New)));
                        result = engToKor(splitKor2Eng(Old) + kor2eng(New));
                    }
                    else {
                        result = Old + New;
                    }
                }
                else {
                    result = Old + New;
                }
                /*
                 else {
                    if(Old.length()==1 && isCho(Old))
                        result = engToKor(kor2eng(Old+New));
                    else
                        result = engToKor(splitKor2Eng(Old) + kor2eng(New));
                 */

            }
            else {
                if (New.equals("ic_backspace_white_48dp"))
                    result = "";
                else if(FirstNew >= 0x1100 && FirstNew <= 0x11FF){
                    result = New;
                }
                else {
                    result = New;
                }
            }
            return result;
        }

        public static boolean isCho(String letter){
            return "ᄀᄁᄂᄃᄄᄅᄆᄇᄈᄉᄊᄋᄌᄍᄎᄏᄐᄑᄒ".contains(letter);
        }

        public static String kor2eng(String letter){
            //String kor = "ㄱㄲㄴㄷㄸㄹㅁᄇㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ";
            String kor = "ᄀᄁᄂᄃᄄᄅᄆᄇᄈᄉᄊᄋᄌᄍᄎᄏᄐᄑ하ᅢᅣᅤᅥᅦᅧᅨᅩᅪᅫᅬᅭᅮᅯᅰᅱᅲᅳᅴᅵᆨᆩᆪᆫᆬᆭᆮᆯᆰᆱᆲᆳᆴᆵᆶᆷᆸᆹᆺᆻᆼᆽᆾᆿᇀᇁᇂ";
            String[] eng = {"r","R","s","e","E","f","a","q","Q","t","T","d","w","W","c","z","x","v","g",
                    "k","o","i","O","j","p","u","P","h","hk","ho","hl","y","n","nj","np","nl","b","m","ml","l",
                    "r","R","rt","s","sw","sg","e","f","fr","fa","fq","ft","fx","fb","fg","a","q","qt","t","T","d","w","c","z","x","v","g"
            };
            String result = "";
            if(letter.length()==1)
                result = eng[kor.indexOf(letter)];
            else {
                for(int i = 0; i<letter.length();i++){
                    result += eng[kor.indexOf(letter.charAt(i))];
                }
            }

            return result;
        }

        public static String kor2eng(char letter){
            return kor2eng(String.valueOf(letter));
        }

    public static String eng2kor(String letter){
        //String kor = "ㄱㄲㄴㄷㄸㄹㅁᄇㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ";
        String kor = "ᄀᄁᄂᄃᄄᄅᄆᄇᄈᄉᄊᄋᄌᄍᄎᄏᄐᄑ하ᅢᅣᅤᅥᅦᅧᅨᅩᅪᅫᅬᅭᅮᅯᅰᅱᅲᅳᅴᅵᆨᆩᆪᆫᆬᆭᆮᆯᆰᆱᆲᆳᆴᆵᆶᆷᆸᆹᆺᆻᆼᆽᆾᆿᇀᇁᇂ";
        String[] eng = {"r","R","s","e","E","f","a","q","Q","t","T","d","w","W","c","z","x","v","g",
                "k","o","i","O","j","p","u","P","h","hk","ho","hl","y","n","nj","np","nl","b","m","ml","l",
                "r","R","rt","s","sw","sg","e","f","fr","fa","fq","ft","fx","fb","fg","a","q","qt","t","T","d","w","c","z","x","v","g"
        };


        String result = "";
        if(letter.length()==1)
            result = String.valueOf(kor.charAt(Arrays.asList(eng).indexOf(letter)));
        else {
            for(int i = 0; i<letter.length();i++){
                result += String.valueOf(kor.charAt(Arrays.asList(eng).indexOf(String.valueOf(letter.charAt(i)))));
            }
        }

        return result;
    }

    public static String eng2kor(char letter){
        return eng2kor(String.valueOf(letter));
    }

        public static String Backspace(String text){

            if(text.length()==0)
                return "";

            String result = text.substring(0,text.length()-1);
            char test = text.charAt(text.length()-1);

            if(test >= 0xAC00 && test<= 0xD7A3){
                String backspaced = splitKor2Eng(String.valueOf(test)).replaceFirst(".$", "");
                Log.d("Backspace: ", "Backspace: "+backspaced);
                result += isCho(eng2kor(backspaced))? eng2kor(backspaced) : engToKor(backspaced);
            }

            return result;
        }

    public static String replaceCho(String text){

        String last = String.valueOf(text.charAt(text.length()-1));
        String cho = "깋낗닣딯띻맇밓빟삫싷앃잏짛찧칳킿팋핗힣";
        String cho2 = "ᄀᄁᄂᄃᄄᄅᄆᄇᄈᄉᄊᄋᄌᄍᄎᄏᄐᄑᄒ";

        return cho.contains(last)? text.substring(0,text.length()-1)+String.valueOf(cho2.charAt(cho.indexOf(last))) : text;
    }

        public static String splitKor2Eng(String tempStr) {
            if(tempStr.length()==0)
                return "";

            String result = "";
            for (int i = 0; i < tempStr.length(); i++) {
                char test = tempStr.charAt(i);

                if (test >= 0xAC00 && test<= 0xD7A3) {
                    char uniVal = (char) (test - 0xAC00);

                    String cho = String.valueOf((char) ((((uniVal - (uniVal % 28)) / 28) / 21) + 0x1100));
                    String jun = String.valueOf((char) ((((uniVal - (uniVal % 28)) / 28) % 21) + 0x1161));
                    String jon = String.valueOf((char) ((uniVal % 28)+ 0x11a7));
                    Log.d("splitkor", "splitKor: "+cho+"|"+jun+"|"+jon);
                    result += kor2eng(cho);
                    if(jun!="ᅵ" || jon !="ᇂ"){
                        result += kor2eng(jun);
                        if(!jon.equals("ᆧ"))
                            result += kor2eng(jon);
                    }

                }
                else if(test>=0x0061 && test<=0x007A){
                    result += String.valueOf(test);
                }
            }

            return result;

        }

    public String combineKor(List<Map<String, Integer>> list) {
        String lastStr = "";
        for (int i = 0; i < list.size(); i++) {
            int a = (int) (list.get(i)).get("cho");
            int b = (int) (list.get(i)).get("jun");
            int c = (int) (list.get(i)).get("jon");

            char temp = (char) (0xAC00 + 28 * 21 * (a) + 28 * (b) + (c));

            lastStr = lastStr.concat(String.valueOf(temp));
            //System.out.println(""+ (char)(0xAC00 + 28 * 21 *(a) + 28 * (b) + (c) ));

        }

        return "" + lastStr;

    }




    // 코드타입 - 초성, 중성, 종성
    static enum CodeType {
        chosung,
        jungsung,
        jongsung
    }
    static String ignoreChars = "`1234567890-=[]\\;',./~!@#$%^&*()_+{}|:\"<>? ";




    /** * 영어를 한글로... */
    public static String engToKor(String eng) {
        StringBuffer sb = new StringBuffer();
        int initialCode = 0, medialCode = 0, finalCode = 0;
        int tempMedialCode, tempFinalCode;
        for (int i = 0; i < eng.length(); i++) {
            // 숫자특수문자 처리
            if (ignoreChars.indexOf(eng.substring(i, i + 1)) > -1) {
                sb.append(eng.substring(i, i + 1));
                continue;
            }
            // 초성코드 추출
            initialCode = getCode(CodeType.chosung, eng.substring(i, i + 1));
            i++;// 다음문자로
            // 중성코드 추출
            tempMedialCode = getDoubleMedial(i, eng);
            // 두 자로 이루어진 중성코드 추출
            if (tempMedialCode != -1) {
                medialCode = tempMedialCode;
                i += 2;
            } else {
                // 없다면,
                medialCode = getSingleMedial(i, eng); // 한 자로 이루어진 중성코드 추출
                i++;
            } // 종성코드 추출
            tempFinalCode = getDoubleFinal(i, eng); // 두 자로 이루어진 종성코드 추출
            if (tempFinalCode != -1) {
                finalCode = tempFinalCode; // 그 다음의 중성 문자에 대한 코드를 추출한다.
                tempMedialCode = getSingleMedial(i + 2, eng);
                if (tempMedialCode != -1) { // 코드 값이 있을 경우
                    finalCode = getSingleFinal(i, eng); // 종성 코드 값을 저장한다.
                } else {
                    i++;
                }
            } else { // 코드 값이 없을 경우 ,
                tempMedialCode = getSingleMedial(i + 1, eng);
                // 그 다음의 중성 문자에 대한 코드 추출.
                if (tempMedialCode != -1) { // 그 다음에 중성 문자가 존재할 경우,
                    finalCode = 0; // 종성 문자는 없음.
                    i--;
                } else {
                    finalCode = getSingleFinal(i, eng); // 종성 문자 추출
                    if (finalCode == -1) {
                        finalCode = 0;
                        i--; // 초성,중성 + 숫자,특수문자, //기호가 나오는 경우 index를 줄임.
                    }
                }
            } // 추출한 초성 문자 코드, //중성 문자 코드, 종성 문자 코드를 합한 후 변환하여 스트링버퍼에 넘김


            sb.append((char)(0xAC00 + initialCode + medialCode + finalCode));
        }
        return sb.toString();
    }
    /** * 해당 문자에 따른 코드를 추출한다. * * @param type * 초성 : chosung, 중성 : jungsung, 종성 : jongsung 구분 * @param char 해당 문자 */
    static private int getCode(CodeType type, String c) {
        String init = "rRseEfaqQtTdwWczxvg"; // 초성
        String[] mid = { // 중성
                "k",
                "o",
                "i",
                "O",
                "j",
                "p",
                "u",
                "P",
                "h",
                "hk",
                "ho",
                "hl",
                "y",
                "n",
                "nj",
                "np",
                "nl",
                "b",
                "m",
                "ml",
                "l"
        }; // 종성
        String[] fin = {
                "r",
                "R",
                "rt",
                "s",
                "sw",
                "sg",
                "e",
                "f",
                "fr",
                "fa",
                "fq",
                "ft",
                "fx",
                "fv",
                "fg",
                "a",
                "q",
                "qt",
                "t",
                "T",
                "d",
                "w",
                "c",
                "z",
                "x",
                "v",
                "g"
        };
        switch (type) {
            case chosung:
                int index = init.indexOf(c);
                if (index != -1) {
                    return index * 21 * 28;
                }
                break;
            case jungsung:
                for (int i = 0; i < mid.length; i++) {
                    if (mid[i].equals(c)) {
                        return i * 28;
                    }
                }
                break;
            case jongsung:
                for (int i = 0; i < fin.length; i++) {
                    if (fin[i].equals(c)) {
                        return i + 1;
                    }
                }
                break;
            default:
                System.out.println("잘못된 타입 입니다");
        }
        return -1;
    }
    static private int getSingleMedial(int i, String eng) {
        if ((i + 1) <= eng.length()) {
            return getCode(CodeType.jungsung, eng.substring(i, i + 1));
        } else {
            return -1;
        }
    } // 한 자로 된 중성값을 리턴한다 // 인덱스를 벗어낫다면 -1을 리턴
    static private int getDoubleMedial(int i, String eng) {
        int result;
        if ((i + 2) > eng.length()) {
            return -1;
        } else {
            result = getCode(CodeType.jungsung, eng.substring(i, i + 2));
            if (result != -1) {
                return result;
            } else {
                return -1;
            }
        }
    } // 두 자로 된 중성을 체크하고, 있다면 값을 리턴한다. // 없으면 리턴값은 -1
    static private int getSingleFinal(int i, String eng) {
        if ((i + 1) <= eng.length()) {
            return getCode(CodeType.jongsung, eng.substring(i, i + 1));
        } else {
            return -1;
        }
    } // 한 자로된 종성값을 리턴한다 // 인덱스를 벗어낫다면 -1을 리턴
    static private int getDoubleFinal(int i, String eng) {
        if ((i + 2) > eng.length()) {
            return -1;
        } else {
            return getCode(CodeType.jongsung, eng.substring(i, i + 2));
        }
    } // 두 자로된 종성을 체크하고, 있다면 값을 리턴한다. // 없으면 리턴값은 -1

}