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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import javax.imageio.ImageIO;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.UserAttrDao;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.util.QRCodeException;
import org.savapage.core.util.QRCodeHelper;
import org.savapage.ext.payment.PaymentGatewayException;
import org.savapage.ext.payment.PaymentGatewayPlugin;
import org.savapage.ext.payment.PaymentGatewayPlugin.PaymentRequest;
import org.savapage.ext.payment.PaymentGatewayTrx;
import org.savapage.ext.payment.bitcoin.BitcoinGateway;
import org.savapage.server.WebApp;
import org.savapage.server.ext.ServerPluginManager;
import org.savapage.server.pages.MarkupHelper;
import org.savapage.server.pages.MessageContent;
import org.savapage.server.session.SpSession;

import net.iharder.Base64;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class AccountBitcoinTransfer extends AbstractUserPage {

    private static final long serialVersionUID = 1L;

    /**
     * Creates and displays a bitcoin payment address in
     * <a href="https://en.bitcoin.it/wiki/BIP_0021">BIP 0021</a> format.
     */
    public AccountBitcoinTransfer(final PageParameters parameters) {

        super(parameters);

        final BitcoinGateway plugin =
                WebApp.get().getPluginManager().getBitcoinGateway();

        if (!plugin.isOnline()) {
            setResponsePage(new MessageContent(AppLogLevelEnum.INFO,
                    this.localized("msg-bitcoin-disabled")));
            return;
        }

        final MarkupHelper helper = new MarkupHelper(this);

        //
        helper.addModifyLabelAttr("money-transfer-gateway", "value",
                plugin.getId());

        //
        final String userId = SpSession.get().getUserId();

        final DaoContext daoCtx = ServiceContext.getDaoContext();
        daoCtx.beginTransaction();

        try {

            final String address = getBitcoinAddress(daoCtx, plugin, userId);

            final String message =
                    String.format("Increment SavaPage account of %s.", userId);

            StringBuilder builder = new StringBuilder();

            builder.append("bitcoin:").append(address).append("?message=")
                    .append(URLEncoder.encode(message, "UTF-8")
                            .replaceAll("\\+", "%20"));

            final URI uri = new URI(builder.toString());

            builder = new StringBuilder(1024);
            builder.append("data:image/png;base64,")
                    .append(createQrCodePngBase64(uri.toString(), 200));

            helper.addModifyLabelAttr("qr-code", "src", builder.toString());

            helper.addModifyLabelAttr("bitcoin-uri-button",
                    localized("button-start"), "href", uri.toString());
            helper.addLabel("bitcoin-trx-id", address);

        } catch (URISyntaxException | IOException | PaymentGatewayException
                | QRCodeException e) {

            setResponsePage(
                    new MessageContent(AppLogLevelEnum.ERROR, e.getMessage()));

        } finally {
            // Release the user lock.
            daoCtx.rollback();
        }

    }

    /**
     *
     * @param daoCtx
     * @param plugin
     * @param userId
     * @return
     * @throws IOException
     * @throws PaymentGatewayException
     */
    private static String getBitcoinAddress(final DaoContext daoCtx,
            final PaymentGatewayPlugin plugin, final String userId)
            throws IOException, PaymentGatewayException {

        final UserService userService =
                ServiceContext.getServiceFactory().getUserService();
        final UserAttrDao userAttrDao = daoCtx.getUserAttrDao();

        final String address;

        /*
         * Lock the user.
         */
        final org.savapage.core.jpa.User user =
                userService.lockByUserId(userId);

        /*
         * Use assigned bitcoin address when present.
         */
        final UserAttr bitcoinAddr = userAttrDao.findByName(user,
                UserAttrEnum.BITCOIN_PAYMENT_ADDRESS);

        if (bitcoinAddr == null) {

            /*
             * Create bitcoin address and save as User attribute.
             */
            final PaymentRequest req = new PaymentRequest();
            req.setUserId(userId);
            req.setCallbackUrl(ServerPluginManager.getCallBackUrl(plugin));
            req.setCurrency(ConfigManager.getAppCurrency());

            final PaymentGatewayTrx trx = plugin.onPaymentRequest(req);
            address = trx.getTransactionId();

            final UserAttr attr = new UserAttr();
            attr.setName(UserAttrEnum.BITCOIN_PAYMENT_ADDRESS.getName());
            attr.setUser(user);
            attr.setValue(address);
            userAttrDao.create(attr);

            daoCtx.commit();

        } else {
            address = bitcoinAddr.getValue();
        }

        return address;
    }

    /**
     *
     * @param codeText
     *            QR code text.
     * @param squareWidth
     *            width and height in pixels.
     * @return The base64 encoded PNG file with QR code
     * @throws QRCodeException
     *             If error.
     */
    private static String createQrCodePngBase64(final String codeText,
            final int squareWidth) throws QRCodeException {

        final BufferedImage image =
                QRCodeHelper.createImage(codeText, squareWidth, null);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                OutputStream b64 = new Base64.OutputStream(out)) {

            ImageIO.write(image, "png", b64);
            return out.toString();

        } catch (IOException e) {
            throw new QRCodeException(e.getMessage());
        }

    }

}
