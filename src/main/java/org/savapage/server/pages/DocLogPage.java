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
package org.savapage.server.pages;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.WebAppTypeEnum;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.helpers.DocLogPagerReq;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.ServiceContext;
import org.savapage.server.session.SpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocLogPage extends AbstractListPage {

    private static final long serialVersionUID = 1L;

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DocLogPage.class);

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /**
     * Maximum number of pages in the navigation bar. IMPORTANT: this must be an
     * ODD number.
     */
    private static final int MAX_PAGES_IN_NAVBAR = 5;

    @Override
    protected boolean needMembership() {
        return false;
    }

    /**
     * @param parameters
     *            Page parameters.
     */
    public DocLogPage(final PageParameters parameters) {

        super(parameters);

        final boolean isAccountsEditor;

        if (this.getSessionWebAppType() == WebAppTypeEnum.ADMIN) {
            this.probePermissionToRead(ACLOidEnum.A_DOCUMENTS);
            isAccountsEditor = this.hasPermissionToEdit(ACLOidEnum.A_ACCOUNTS);
        } else {
            isAccountsEditor = false;
        }

        final String data = getParmValue(POST_PARM_DATA);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("data : {}", data);
        }

        final boolean showFinancialData;

        final DocLogPagerReq req = DocLogPagerReq.read(data);

        final DocLogDao.Type docType = req.getSelect().getDocType();

        final Long userId;
        final UserIdDto sessionUserIdDto = SpSession.get().getUserIdDto();

        final WebAppTypeEnum webAppType = this.getSessionWebAppType();

        if (webAppType == WebAppTypeEnum.JOBTICKETS
                || webAppType == WebAppTypeEnum.PRINTSITE) {

            showFinancialData = true;
            userId = req.getSelect().getUserId();

        } else if (req.getSelect().getAccountId() == null) {

            if (this.getSessionWebAppType() == WebAppTypeEnum.ADMIN) {
                userId = req.getSelect().getUserId();
                showFinancialData = true;
            } else {
                /*
                 * If we are called in a User WebApp context we ALWAYS use the
                 * user of the current session.
                 */
                userId = SpSession.get().getUserDbKey();

                showFinancialData = ACCESS_CONTROL_SERVICE
                        .hasAccess(sessionUserIdDto, ACLOidEnum.U_FINANCIAL);
            }
        } else {
            showFinancialData = true;
            userId = null;
        }

        final Integer userQueueJournalPrivilege = ACCESS_CONTROL_SERVICE
                .getPrivileges(sessionUserIdDto, ACLOidEnum.U_QUEUE_JOURNAL);

        final boolean isTicketReopen = webAppType == WebAppTypeEnum.JOBTICKETS
                && ConfigManager.instance()
                        .isConfigValue(Key.WEBAPP_JOBTICKETS_REOPEN_ENABLE);

        final EntityManager em = DaoContextImpl.peekEntityManager();
        final DocLogItem.AbstractQuery query = DocLogItem.createQuery(docType);
        final long logCount = query.filteredCount(em, userId, req);

        /*
         * Display the requested page.
         */
        final List<DocLogItem> entryList =
                query.getListChunk(em, userId, req, getLocale());

        add(new PropertyListView<DocLogItem>("doc-entry-view", entryList) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<DocLogItem> item) {

                /*
                 * Step 1: Create panel and add to page.
                 */
                final DocLogItemPanel panel = new DocLogItemPanel("doc-entry",
                        item.getModel(), showFinancialData, isTicketReopen,
                        isAccountsEditor, userQueueJournalPrivilege);

                item.add(panel);

                /*
                 * Step 2: populate the panel.
                 *
                 * Reason: “If the component is not an instance of Page then it
                 * must be a component that has already been added to a page.”
                 * otherwise it will throw the following warning message.
                 *
                 * 12:07:11,726 WARN [Localizer] Tried to retrieve a localized
                 * string for a component that has not yet been added to the
                 * page.
                 *
                 * See:
                 * http://jaibeermalik.wordpress.com/2008/11/12/localization
                 * -of-wicket-applications/
                 */
                panel.populate(item.getModel());
            }
        });

        // Display the navigation bars and write the response.
        createNavBarResponse(req, logCount, MAX_PAGES_IN_NAVBAR,
                "sp-doclog-page", new String[] { "nav-bar-1", "nav-bar-2" });
        //
        final ConfirmationPopupPanel pnl =
                new ConfirmationPopupPanel("doclog-store-delete-popup");
        pnl.populate("sp-doclog-store-delete-popup",
                CommunityDictEnum.DOC_STORE.getWord(getLocale()),
                PhraseEnum.Q_DELETE_DOCUMENT.uiText(getLocale()));
        this.add(pnl);
    }
}
