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

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.PrintModeEnum;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrintersBase extends AbstractAdminPage {

    /**
     * Version for serialization.
     */
    private static final long serialVersionUID = 1L;

    /** */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final String WICKET_ID_BUTTON_BAR = "button-bar";
    /** */
    private static final String WICKET_ID_BUTTON_CUPS = "btn-cups";

    /** */
    private static final String WICKET_ID_BUTTON_SYNC = "btn-sync";

    /** */
    private static final String WICKET_ID_BUTTON_SNMP = "btn-snmp-all";

    /**
     * @param parameters
     *            The page parameters.
     */
    public PrintersBase(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);

        if (this.probePermissionToEdit(ACLOidEnum.A_PRINTERS)) {

            helper.addTransparant(WICKET_ID_BUTTON_BAR);
            helper.addButton(WICKET_ID_BUTTON_SYNC, HtmlButtonEnum.SYNCHRONIZE);

            final boolean linkCUPS = this.isIntranetRequest();

            final Label label = helper.encloseLabel(WICKET_ID_BUTTON_CUPS,
                    "CUPS", linkCUPS);

            if (linkCUPS) {
                MarkupHelper.modifyLabelAttr(label, MarkupHelper.ATTR_HREF,
                        PROXY_PRINT_SERVICE.getCupsAdminUrl().toString());
            }

            helper.encloseLabel(WICKET_ID_BUTTON_SNMP,
                    NounEnum.STATUS.uiText(getLocale()), ConfigManager
                            .instance().isConfigValue(Key.PRINTER_SNMP_ENABLE));

        } else {
            helper.discloseLabel(WICKET_ID_BUTTON_BAR);
        }

        helper.addLabel("title", ACLOidEnum.A_PRINTERS.uiText(getLocale()));

        helper.addLabel("select-and-sort", PhraseEnum.SELECT_AND_SORT);

        helper.addLabel("prompt-name", NounEnum.NAME);

        helper.addLabel("group-prompt", NounEnum.GROUP);
        helper.addLabel("prompt-printer-status", NounEnum.STATUS);

        helper.addLabel("prompt-printer-jobticket", NounEnum.MODE);
        helper.addLabel("printer-jobticket-n", NounEnum.PRINTER);
        helper.addLabel("printer-jobticket-y",
                PrintModeEnum.TICKET.uiText(getLocale()));

        helper.addLabel("prompt-printer-internal", NounEnum.SCOPE);
        helper.addLabel("printer-internal-n", AdjectiveEnum.PUBLIC);
        helper.addLabel("printer-internal-y", AdjectiveEnum.INTERNAL);

        helper.addButton("button-apply", HtmlButtonEnum.APPLY);
        helper.addButton("button-default", HtmlButtonEnum.DEFAULT);

        helper.addLabel("printer-status-enabled", AdverbEnum.ENABLED);
        helper.addLabel("printer-status-disabled", AdverbEnum.DISABLED);

        helper.addLabel("prompt-printer-deleted", AdverbEnum.DELETED);
        helper.addLabel("printer-deleted-y", AdverbEnum.DELETED);
        helper.addLabel("printer-deleted-n", AdjectiveEnum.ACTIVE);

        helper.addLabel("prompt-sort-by", NounEnum.SORTING);
        helper.addLabel("sort-asc", AdjectiveEnum.ASCENDING);
        helper.addLabel("sort-desc", AdjectiveEnum.DESCENDING);

    }

}
