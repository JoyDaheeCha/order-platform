package com.flab.orderplatform.order.application.command;

import lombok.Getter;

@Getter
public abstract class IdempotentKeyCommand{
    String idempotentKey;
}
