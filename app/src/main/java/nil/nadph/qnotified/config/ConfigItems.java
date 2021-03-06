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
package nil.nadph.qnotified.config;

import nil.nadph.qnotified.ExfriendManager;
import nil.nadph.qnotified.util.MainProcess;
import nil.nadph.qnotified.util.Utils;

import java.io.IOException;

import static nil.nadph.qnotified.util.Utils.QN_VERSION_CODE;
import static nil.nadph.qnotified.util.Utils.log;

public class ConfigItems {
    public static final String qn_hide_msg_list_miniapp = "qn_hide_msg_list_miniapp";
    public static final String qn_hide_ex_entry_group = "qn_hide_ex_entry_group";
    public static final String qn_muted_at_all = "qn_muted_at_all";
    public static final String qn_muted_red_packet = "qn_muted_red_packet";
    public static final String qn_mute_talk_back = "qn_mute_talk_back";
    public static final String qn_disable_hot_patch = "qn_disable_hot_patch";
    public static final String qn_file_recv_redirect_enable = "qn_file_recv_redirect_enable";
    public static final String qn_file_recv_redirect_path = "qn_file_recv_redirect_path";
    public static final String qn_fake_bat_expr = "qn_fake_bat_expr";
    public static final String cfg_nice_user = "cfg_nice_user";
    public static final String cache_qn_prev_version = "cache_qn_prev_version";

    public static final SwitchConfigItem qn_notify_when_del = new SwitchConfigItem() {
        @Override
        public boolean isValid() {
            try {
                ExfriendManager.getCurrent();
                return true;
            } catch (IllegalArgumentException e) {
                //not login
                return false;
            }
        }

        @Override
        public boolean isEnabled() {
            try {
                ExfriendManager mgr = ExfriendManager.getCurrent();
                return mgr.isNotifyWhenDeleted();
            } catch (IllegalArgumentException e) {
                //not login
                return false;
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            try {
                ExfriendManager mgr = ExfriendManager.getCurrent();
                mgr.setNotifyWhenDeleted(enabled);
            } catch (IllegalArgumentException e) {
                log(e);
            }
        }
    };

    @MainProcess
    public static void removePreviousCacheIfNecessary() {
        if (!Utils.__REMOVE_PREVIOUS_CACHE) return;
        ConfigManager cache = ConfigManager.getCache();
        if (cache.getIntOrDefault(cache_qn_prev_version, -1) < Utils.QN_VERSION_CODE) {
            try {
                cache.getFile().delete();
                cache.reinit();
            } catch (IOException e) {
                log(e);
            }
            cache.putInt(cache_qn_prev_version, QN_VERSION_CODE);
            try {
                cache.save();
            } catch (IOException e) {
                log(e);
            }
        }
    }
}
