package com.deepreach.common.core.event;

import com.deepreach.common.core.domain.entity.SysUser;
import org.springframework.context.ApplicationEvent;

/**
 * 商家用户创建事件。
 */
public class MerchantCreatedEvent extends ApplicationEvent {

    private final SysUser merchant;

    public MerchantCreatedEvent(Object source, SysUser merchant) {
        super(source);
        this.merchant = merchant;
    }

    public SysUser getMerchant() {
        return merchant;
    }
}
