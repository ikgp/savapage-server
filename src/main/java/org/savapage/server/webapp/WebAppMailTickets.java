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
package org.savapage.server.webapp;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.WebAppTypeEnum;

public final class WebAppMailTickets extends WebAppUser {

    /** */
    private static final long serialVersionUID = 5608503008706484277L;

    /**
     *
     * @param parameters
     *            The {@link PageParameters}.
     */
    public WebAppMailTickets(final PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected IConfigProp.Key getInternetEnableKey() {
        return IConfigProp.Key.WEBAPP_INTERNET_MAILTICKETS_ENABLE;
    }

    @Override
    public WebAppTypeEnum getWebAppType() {
        return WebAppTypeEnum.MAILTICKETS;
    }

}
