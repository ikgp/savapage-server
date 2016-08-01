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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.community.MemberCard;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class CommunityStatusFooterPanel extends Panel {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param id
     *            The panel identification.
     */
    public CommunityStatusFooterPanel(final String id) {

        super(id);

        final MarkupHelper helper = new MarkupHelper(this);

        //
        final MemberCard card = MemberCard.instance();
        final String memberStatus = card.getStatusUserText(getLocale());
        final boolean cardDesirable = card.isMembershipDesirable();

        if (card.isVisitorCard()) {
            helper.encloseLabel("membership-org", memberStatus, !cardDesirable);
        } else {
            helper.encloseLabel("membership-org", card.getMemberOrganisation(),
                    StringUtils.isNotBlank(card.getMemberOrganisation()));
        }

        helper.encloseLabel("membership-status", memberStatus, cardDesirable);
    }
}
