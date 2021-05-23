package com.esplugin.module.xpackPlugin;

import com.esplugin.action.TcpAuditActionFilter;
import org.elasticsearch.common.inject.AbstractModule;

/**
 * @ClassName EsModule
 * @Description 请描述类的业务用途
 * @Author yangsongbai
 * @Date 2021/5/23 下午1:58
 * @email yangsongbaivat@163.com
 * @Version 1.0
 **/
public class EsModule  extends AbstractModule {
    protected void configure() {
        bind(TcpAuditActionFilter.class).asEagerSingleton();
    }
}
