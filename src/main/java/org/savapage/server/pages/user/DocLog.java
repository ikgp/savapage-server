/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages.user;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.helpers.HtmlButtonEnum;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.session.SpSession;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class DocLog extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /**
     *
     * @param parameters
     *            The page parameters.
     */
    public DocLog(final PageParameters parameters) {

        super(parameters);

        final MarkupHelper helper = new MarkupHelper(this);
        final String wicketId = "button-transactions";

        if (ACCESS_CONTROL_SERVICE.hasAccess(SpSession.get().getUserIdDto(),
                ACLOidEnum.U_FINANCIAL)) {
            helper.addLabel(wicketId,
                    NounEnum.TRANSACTION.uiText(getLocale(), true));
        } else {
            helper.discloseLabel(wicketId);
        }

        helper.encloseLabel("btn-txt-gdpr", "GDPR", ConfigManager.instance()
                .isConfigValue(Key.WEBAPP_USER_GDPR_ENABLE));

        helper.addLabel("title", NounEnum.DOCUMENT.uiText(getLocale(), true));
        helper.addButton("button-back", HtmlButtonEnum.BACK);
    }

}
