package org.ipring.util;

import org.ipring.client.impl.BaseServiceManagerImpl;
import org.ipring.client.BaseServiceManager;
import org.ipring.client.baseservice.SendEmailSubmit;

public class SendEmailUtil {

    public static void sendCustomEmail(String email, String subject, String content) {
        BaseServiceManager baseServiceManager = SpringContextUtils.getBean(BaseServiceManagerImpl.class);
        SendEmailSubmit sendEmailSubmit = new SendEmailSubmit();
        sendEmailSubmit.setProductType(26);
        sendEmailSubmit.setSmtpType(2);
        sendEmailSubmit.setEmail(email);
        sendEmailSubmit.setCustomSubject(subject);
        sendEmailSubmit.setCustomContent(content);
        baseServiceManager.sendEmail(sendEmailSubmit);
    }
}
