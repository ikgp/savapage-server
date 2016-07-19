/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.MissingResourceException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AppAboutPanel extends Panel {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    private static final boolean SHOW_TRANSLATOR_INFO = false;

    /**
     * Gives the localized string for a key.
     *
     * @param key
     *            The key from the XML resource file
     * @return The localized string.
     */
    private final String localized(final String key) {
        return getLocalizer().getString(key, this);
    }

    /**
     * Localizes and format a string with placeholder arguments.
     *
     * @param key
     *            The key from the XML resource file
     * @param objects
     *            The values to fill the placeholders
     * @return The localized string.
     */
    protected final String localized(final String key,
            final Object... objects) {
        return MessageFormat.format(getLocalizer().getString(key, this),
                objects);
    }

    /**
     *
     */
    public AppAboutPanel(final String id) {

        super(id);

        //
        add(new Label("current-year",
                String.valueOf(Calendar.getInstance().get(Calendar.YEAR))));

        add(new Label("app-name", CommunityDictEnum.SAVAPAGE.getWord()));

        Label labelWrk;

        //
        add(new Label("app-copyright-owner",
                CommunityDictEnum.DATRAVERSE_BV.getWord()));

        //
        labelWrk = new Label("savapage-source-code-url",
                localized("source-code-link"));
        labelWrk.add(new AttributeModifier("href",
                CommunityDictEnum.COMMUNITY_SOURCE_CODE_URL.getWord()));
        add(labelWrk);

        //
        final MarkupHelper helper = new MarkupHelper(this);

        final String downloadPanelId = "printerdriver-download-panel";

        if (ConfigManager.instance().isConfigValue(
                IConfigProp.Key.WEBAPP_ABOUT_DRIVER_DOWNLOAD_ENABLE)) {

            final PrinterDriverDownloadPanel downloadPanel =
                    new PrinterDriverDownloadPanel(downloadPanelId);
            add(downloadPanel);
            downloadPanel.populate();

        } else {
            helper.discloseLabel(downloadPanelId);
        }

        //
        String translatorInfo = null;

        if (SHOW_TRANSLATOR_INFO) {
            try {
                translatorInfo = localized("translator-info",
                        localized("_translator_name"));
            } catch (MissingResourceException e) {
                translatorInfo = null;
            }
        }

        add(MarkupHelper.createEncloseLabel("translator-info", translatorInfo,
                StringUtils.isNotBlank(translatorInfo)));

    }

}