/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.server.webapp;

import java.text.MessageFormat;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.community.CommunityDictEnum;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class ZeroPagePanel extends Panel {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * A {@link PageParameters} value indicating that a submit was encountered
     * on the {@link ZeroPagePanel}.
     */
    public static final String PARM_SUBMIT_INIDICATOR = "zero-refresh";

    /**
     *
     * @param id
     *            The id.
     */
    public ZeroPagePanel(final String id) {
        super(id);
    }

    /**
     *
     * @param webAppType
     *            The {@link WebAppTypeEnum} this panel is used in.
     */
    public void populate(final WebAppTypeEnum webAppType) {

        final Form<?> form = new Form<Void>("refresh-page-form") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit() {
                // no code intended
            }
        };

        final AjaxButton continueButton =
                new AjaxButton("refresh-page-submit") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(final AjaxRequestTarget target,
                            final Form<?> form) {

                        final PageParameters parms = new PageParameters();
                        parms.set(PARM_SUBMIT_INIDICATOR, "1");

                        switch (webAppType) {
                        case ADMIN:
                            setResponsePage(WebAppAdminPage.class, parms);
                            break;
                        case POS:
                            setResponsePage(WebAppPosPage.class, parms);
                            break;
                        default:
                            setResponsePage(WebAppUserPage.class, parms);
                            break;
                        }
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target,
                            Form<?> form) {
                        // no code intended
                    }
                };

        form.add(continueButton);
        add(form);

        add(new Label("zero-page-title", MessageFormat.format(this
                .getLocalizer().getString("zero-page-title", this),
                CommunityDictEnum.SAVAPAGE.getWord())));

        continueButton.add(new AttributeModifier("value", this.getLocalizer()
                .getString("button-continue", this)));
    }
}
