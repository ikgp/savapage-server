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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.savapage.core.i18n.PhraseEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrinterDriverDownloadPanel extends Panel {

    /**
     * .
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * @param id
     *            The panel id.
     */
    public PrinterDriverDownloadPanel(final String id) {
        super(id);
    }

    /**
     *
     */
    public void populate() {
        final MarkupHelper helper = new MarkupHelper(this);
        helper.addLabel("windows-driver-msg", PhraseEnum.WINDOWS_DRIVER_MSG);
    }
}
