/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages.admin;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.TooltipPanel;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserGroupsBase extends AbstractAdminPage {

    /** */
    private static final String WICKET_ID_BUTTON_ADD_REMOVE =
            "button-add-remove";

    /** */
    private static final String WICKET_ID_TXT_NOT_READY =
            "warn-not-ready-to-use";

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public UserGroupsBase(final PageParameters parameters) {

        super(parameters);

        final boolean hasEditorAccess =
                this.probePermissionToEdit(ACLOidEnum.A_USERS);

        final MarkupHelper helper = new MarkupHelper(this);

        helper.addLabel("title", NounEnum.GROUP.uiText(getLocale(), true));
        helper.addLabel("select-and-sort", PhraseEnum.SELECT_AND_SORT);

        helper.addLabel("prompt-name-containing-text", NounEnum.NAME);

        helper.addLabel("prompt-sort-by", NounEnum.SORTING);
        helper.addLabel("sort-asc", AdjectiveEnum.ASCENDING);
        helper.addLabel("sort-desc", AdjectiveEnum.DESCENDING);

        helper.addButton("button-apply", HtmlButtonEnum.APPLY);
        helper.addButton("button-default", HtmlButtonEnum.DEFAULT);

        //
        if (ConfigManager.instance().isAppReadyToUse()) {

            if (hasEditorAccess) {

                helper.encloseLabel(WICKET_ID_BUTTON_ADD_REMOVE,
                        localized("button-add-remove"), true);

                final TooltipPanel tooltip =
                        new TooltipPanel("tooltip-add-remove");
                tooltip.populate(localized("tooltip-add-remove"), true);

                add(tooltip);
            } else {
                helper.discloseLabel(WICKET_ID_BUTTON_ADD_REMOVE);
            }

            helper.discloseLabel(WICKET_ID_TXT_NOT_READY);

        } else {
            helper.discloseLabel(WICKET_ID_BUTTON_ADD_REMOVE);
            helper.encloseLabel(WICKET_ID_TXT_NOT_READY,
                    localized("warn-not-ready-to-use"), true);
        }

        /*
         * Reserved for future use.
         */
        // add(new JrExportFileExtButtonPanel("report-button-panel",
        // "sp-btn-user-groups-report"));
    }
}
