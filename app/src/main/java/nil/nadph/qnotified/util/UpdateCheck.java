/* QNotified - An Xposed module for QQ/TIM
 * Copyright (C) 2019-2020 xenonhydride@gmail.com
 * https://github.com/cinit/QNotified
 *
 * This software is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see
 * <https://www.gnu.org/licenses/>.
 */
package nil.nadph.qnotified.util;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import nil.nadph.qnotified.config.ConfigManager;
import nil.nadph.qnotified.ui.CustomDialog;
import nil.nadph.qnotified.ui.ViewBuilder;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static nil.nadph.qnotified.util.Utils.log;

public class UpdateCheck implements View.OnClickListener, Runnable {

    public static final String UPDATE_INFO_GET1 = "https://raw.githubusercontent.com/cinit/QNotified/master/update_info";
    public static final String UPDATE_INFO_GET2 = "https://gitee.com/kernelex/QNotified/raw/master/update_info";
    public static final String qn_update_info = "qn_update_info";
    public static final String qn_update_time = "qn_update_time";
    private final int RL_LOAD = 1;
    private final int RL_SHOW_RET = 2;
    int currVerCode = Utils.QN_VERSION_CODE;
    String currVerName = Utils.QN_VERSION_NAME;
    private ViewGroup viewGroup;
    private boolean clicked = false;
    private PHPArray result;
    private int runlevel;

    public UpdateCheck() {
    }

    public String doRefreshInfo() {
        String content = null;
        int failed = 0;
        try {
            URL reqURL = new URL(UPDATE_INFO_GET2);
            HttpsURLConnection httpsConn = (HttpsURLConnection) reqURL.openConnection();
            InputStream in = httpsConn.getInputStream();
            ByteArrayOutputStream bais = new ByteArrayOutputStream();
            byte[] buf = new byte[256];
            int len;
            while ((len = in.read(buf)) != -1) {
                bais.write(buf, 0, len);
            }
            in.close();
            content = bais.toString("UTF-8");
            httpsConn.disconnect();
            ConfigManager cache = ConfigManager.getCache();
            cache.putString(qn_update_info, content);
            cache.getAllConfig().put(qn_update_time, System.currentTimeMillis() / 1000L);
            cache.save();
            return content;
        } catch (IOException e) {
            //fuck,try another
        }
        try {
            URL reqURL = new URL(UPDATE_INFO_GET1);
            HttpsURLConnection httpsConn = (HttpsURLConnection) reqURL.openConnection();
            InputStream in = httpsConn.getInputStream();
            ByteArrayOutputStream bais = new ByteArrayOutputStream();
            byte[] buf = new byte[256];
            int len;
            while ((len = in.read(buf)) != -1) {
                bais.write(buf, 0, len);
            }
            in.close();
            content = bais.toString("UTF-8");
            httpsConn.disconnect();
            ConfigManager cache = ConfigManager.getCache();
            cache.putString(qn_update_info, content);
            cache.getAllConfig().put(qn_update_time, System.currentTimeMillis() / 1000L);
            cache.save();
            return content;
        } catch (IOException e) {
            final IOException e2 = e;
            runlevel = 0;
            if (content == null)
                new Handler(viewGroup.getContext().getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(viewGroup.getContext(), "检查更新失败:" + e2, Toast.LENGTH_SHORT).show();
                    }
                });
        }
        return null;
    }

    private String getCachedUpdateInfoOrNull() {
        try {
            ConfigManager cache = ConfigManager.getCache();
            String str = cache.getString(qn_update_info);
            long time = cache.getLongOrDefault(qn_update_time, 0);
            return str;
        } catch (Exception e) {
            log(e);
            return null;
        }

    }

    public void setVersionTip(ViewGroup vg) {
        viewGroup = vg;
        try {
            TextView tv_v = viewGroup.findViewById(ViewBuilder.R_ID_VALUE);
            TextView tv_t = viewGroup.findViewById(ViewBuilder.R_ID_TITLE);
            String str = getCachedUpdateInfoOrNull();
            if (str != null) {
                String highest = currVerName;
                int hv = currVerCode;
                long time = 0;
                for (Object obj : PHPArray.fromJson(str)._$_E()) {
                    PHPArray info = (PHPArray) obj;
                    int v = ((Number) info.__("code")._$()).intValue();
                    if (v > hv) {
                        hv = v;
                        highest = info.__("name")._$().toString();
                        time = ((Number) info.__("code")._$()).longValue();
                    }
                }
                if (hv > currVerCode) {
                    //has newer
                    tv_v.setText(highest);
                    tv_v.setTextColor(Color.argb(255, 242, 140, 72));
                    tv_t.setText("有新版本可用");
                    if (clicked) {
                        doShowUpdateInfo();
                    }
                } else {
                    if (System.currentTimeMillis() / 1000L - time < 3 * 24 * 3600)
                        tv_v.setText("已是最新");
                }
            }
        } catch (Exception e) {
            log(e);
        }
    }

    @Override
    public void run() {
        switch (runlevel) {
            case RL_LOAD:
                String ret = doRefreshInfo();
                if (ret == null) return;
                runlevel = 2;
                result = PHPArray.fromJson(ret);
                new Handler(viewGroup.getContext().getMainLooper()).post(this);
                return;
            case RL_SHOW_RET:
                TextView tv_v = viewGroup.findViewById(ViewBuilder.R_ID_VALUE);
                TextView tv_t = viewGroup.findViewById(ViewBuilder.R_ID_TITLE);
                String highest = currVerName;
                int hv = currVerCode;
                for (Object obj : result._$_E()) {
                    PHPArray info = (PHPArray) obj;
                    int v = ((Number) info.__("code")._$()).intValue();
                    if (v > hv) {
                        hv = v;
                        highest = info.__("name")._$().toString();
                    }
                }
                if (hv > currVerCode) {
                    //has newer
                    tv_v.setText(highest);
                    tv_v.setTextColor(Color.argb(255, 242, 140, 72));
                    tv_t.setText("有新版本可用");
                    if (clicked) {
                        doShowUpdateInfo();
                    }
                } else {
                    tv_v.setText("已是最新");
                }
                runlevel = 0;
                if (clicked) doShowUpdateInfo();
                return;
        }
    }


    private void doShowUpdateInfo() {
        try {
            clicked = false;
            Activity ctx = (Activity) viewGroup.getContext();
            CustomDialog dialog = CustomDialog.create(ctx);
            dialog.setTitle("当前" + currVerName + " (" + currVerCode + ")");
            dialog.setCancelable(true);
            //dialog.setNegativeButton("关闭", null);
            dialog.setNegativeButton("关闭", new Utils.DummyCallback());
			/*PopupWindow pop=new PopupWindow();
			 pop.setWidth(WRAP_CONTENT);
			 pop.setHeight(WRAP_CONTENT);*/
            //LinearLayout main = new LinearLayout(ctx);
            //pop.setContentView(main);
            //main.setOrientation(LinearLayout.VERTICAL);
            //ScrollView scrollView = new ScrollView(ctx);
            //.setView(scrollView);
            //scrollView.addView(main, WRAP_CONTENT, WRAP_CONTENT);
            SpannableStringBuilder sb = new SpannableStringBuilder();
            //StringBuilder sb=new StringBuilder();
            //TextView list = new TextView(ctx);
            //main.addView(list, WRAP_CONTENT, WRAP_CONTENT);
            //list.setAutoLinkMask(Linkify.WEB_URLS);
            for (Object obj : result._$_E()) {
                PHPArray ver = (PHPArray) obj;
                String vn = (String) ver.__("name")._$();
                int vc = ((Number) ver.__("code")._$()).intValue();
                String desc = "" + ver.__("desc")._$();
                String md5 = (String) ver.__("md5")._$();
                long time = ((Number) ver.__("time")._$()).longValue();
                String date = Utils.getRelTimeStrSec(time);
                boolean taichi = ver.__("taichi")._$b();
                boolean beta = ver.__("beta")._$b();
                SpannableString tmp = new SpannableString(vn + " (" + vc + ")");
                tmp.setSpan(new RelativeSizeSpan(1.8f), 0, tmp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.append(tmp);
                switch (Utils.sign(vc - currVerCode)) {
                    case 0:
                        sb.append("当前版本");
                        break;
                    case -1:
                        sb.append("旧版本");
                        break;
                    case 1:
                        sb.append("新版本");
                }
                sb.append("\n发布于").append(date);
                sb.append(beta ? " (测试版) " : "");
                sb.append('\n');
                if (taichi) sb.append("已适配太极\n");
                sb.append("md5:").append(md5).append("\n");
                sb.append(desc);
                sb.append("\n下载地址:\n");
                for (Object obj2 : ver.__("urls")._$_E()) {
                    tmp = new SpannableString((String) obj2);
                    tmp.setSpan(new URLSpan((String) obj2), 0, tmp.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.append(tmp);
                    sb.append("\n");
                }
                sb.append("\n");
            }
            //list.setText(sb);
            dialog.setMessage(sb);
            TextView tv = dialog.getMessageTextView();
            if (tv != null) {
                tv.setLinksClickable(true);
                tv.setEnabled(true);
                tv.setFocusable(true);
                try {
                    tv.setFocusableInTouchMode(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        tv.setTextIsSelectable(true);
                    }
                    tv.setAutoLinkMask(Linkify.WEB_URLS);
                } catch (NoSuchMethodError ignored) {
                }
            }
            dialog.show();
        } catch (Exception e) {
            log(e);
        }
    }

    @Override
    public void onClick(View v) {
        viewGroup = (ViewGroup) v;
        clicked = true;
        if (result == null) {
            runlevel = 1;
            new Thread(this).start();
        } else {
            doShowUpdateInfo();
        }
    }
}
