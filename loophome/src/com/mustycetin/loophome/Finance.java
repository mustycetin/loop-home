package com.mustycetin.loophome;

import java.util.ArrayList;
import org.json.JSONObject;

final class Finance {
    private static final String[][] ITEMS = {new String[]{"gram-altin", "Gram Altın"}, new String[]{"ata-altin", "Ata Altın"}, new String[]{"ceyrek-altin", "Çeyrek Altın"}, new String[]{"USD", "Dolar"}, new String[]{"EUR", "Euro"}};

    static final class Row {
        final double change;
        final String name;
        final double price;

        Row(String str, double d, double d2) {
            this.name = str;
            this.price = d;
            this.change = d2;
        }
    }

    private Finance() {
    }

    static Object[] fetch() throws Exception {
        JSONObject jSONObject = new JSONObject(Net.text("https://finans.truncgil.com/today.json"));
        ArrayList arrayList = new ArrayList();
        String[][] strArr = ITEMS;
        int length = strArr.length;
        int r5 = 0;
        while (true) {
            if (r5 >= length) {
                break;
            }
            String[] strArr2 = strArr[r5];
            JSONObject jSONObjectOptJSONObject = jSONObject.optJSONObject(strArr2[0]);
            if (jSONObjectOptJSONObject != null) {
                arrayList.add(new Row(strArr2[1], num(jSONObjectOptJSONObject.optString("Satış")), num(jSONObjectOptJSONObject.optString("Değişim").replace("%", ""))));
            }
            r5++;
        }
        String strOptString = jSONObject.optString("Update_Date");
        Object[] objArr = new Object[2];
        objArr[0] = arrayList;
        objArr[1] = strOptString.length() >= 16 ? strOptString.substring(11, 16) : "";
        return objArr;
    }

    private static double num(String str) {
        try {
            return Double.parseDouble(str.trim().replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0d;
        }
    }
}
