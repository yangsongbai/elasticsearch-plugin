# 开发手册  

## 打包    

```
mvn clean install 
```

## 使用  
```
#安装
bin/elasticsearch-plugin install -b file:///Users/yangsongbai/apps/elasticsearch-6.4.0/elasticsearch-plugin-6.4.0.zip
#卸载
bin/elasticsearch-plugin remove  elasticsearch-plugin   

```

# 注意事项  
如果实现，拦截所有http请求;如com.esplugin.EsPlugin类中重写ActionPlugin的getRestHandlerWrapper，
```
 @Override
    public UnaryOperator<RestHandler> getRestHandlerWrapper(ThreadContext threadContext) {
        return sourceHandler -> (RestHandler) (request, channel, client) -> {
            this.httpAuthHandler.authorize(sourceHandler, request, channel, client);
        };
    }
```
则必须要在elasticsearch.yml中配置 ``xpack.security.enabled: false``,否则会和xpack插件冲突   
原因请参看xpack的实现`org.elasticsearch.xpack.core.LocalStateCompositeXPackPlugin`    
```
 @Override
    public UnaryOperator<RestHandler> getRestHandlerWrapper(ThreadContext threadContext) {

                // There can be only one.
        List<UnaryOperator<RestHandler>> items = filterPlugins(ActionPlugin.class).stream().map(p ->
                p.getRestHandlerWrapper(threadContext)).filter(Objects::nonNull).collect(Collectors.toList());
        if (items.size() > 1) {
            throw new UnsupportedOperationException("Only the security ActionPlugin should override this");
        } else if (items.size() == 1) {
            return items.get(0);
        } else {
            return null;
        }
    }
```
以及源码 ``org.elasticsearch.action.ActionModule`` 构造函数,如果实现两个，在elasticsearch启动时则会报错
```
for (ActionPlugin plugin : actionPlugins) {
            UnaryOperator<RestHandler> newRestWrapper = plugin.getRestHandlerWrapper(threadPool.getThreadContext());
            if (newRestWrapper != null) {
                logger.debug("Using REST wrapper from plugin " + plugin.getClass().getName());
                if (restWrapper != null) {
                    throw new IllegalArgumentException("Cannot have more than one plugin implementing a REST wrapper");
                }
                restWrapper = newRestWrapper;
            }
        }
```
RestHandlerWrapper的最终拦截位置``org.elasticsearch.rest.RestController``的    
``boolean dispatchRequest(final RestRequest request, final RestChannel channel, final NodeClient client, final Optional<RestHandler> mHandler)``    
方法
```
try {
                if (canTripCircuitBreaker(mHandler)) {
                    inFlightRequestsBreaker(circuitBreakerService).addEstimateBytesAndMaybeBreak(contentLength, "<http_request>");
                } else {
                    inFlightRequestsBreaker(circuitBreakerService).addWithoutBreaking(contentLength);
                }
                // iff we could reserve bytes for the request we need to send the response also over this channel
                responseChannel = new ResourceHandlingHttpChannel(channel, circuitBreakerService, contentLength);

                final RestHandler wrappedHandler = mHandler.map(h -> handlerWrapper.apply(h)).get();
                wrappedHandler.handleRequest(request, responseChannel, client);
                requestHandled = true;
            } catch (Exception e) {
                responseChannel.sendResponse(new BytesRestResponse(responseChannel, e));
                // We "handled" the request by returning a response, even though it was an error
                requestHandled = true;
            }
```

# 参考  
  es6.4版本
  https://www.elastic.co/guide/en/elasticsearch/plugins/6.4/plugin-authors.html   
