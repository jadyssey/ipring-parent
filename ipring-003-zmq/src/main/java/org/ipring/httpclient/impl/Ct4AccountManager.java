package org.ipring.httpclient.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipring.httpclient.ICt4AccountManager;
import org.ipring.model.AccountAddParam;
import org.ipring.model.common.Return;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class Ct4AccountManager implements ICt4AccountManager {

    private final ICt4AccountManager iCt4Manager;

    @Override
    public Return<Object> crateAccount(String uid, AccountAddParam accountAddParam) {
        return iCt4Manager.crateAccount(uid, accountAddParam);
    }
}
