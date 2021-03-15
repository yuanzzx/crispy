package com.yuandragon.crispy.netty.server;

import com.yuandragon.crispy.common.setting.CrispySettingConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author: yuanzhanzhenxing
 */
public class NettyServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);

    public void init() throws Exception {
        NettyHttpServerHandler nettyHttpServerHandler = new NettyHttpServerHandler();

        EventLoopGroup parentGroup = new NioEventLoopGroup();
        EventLoopGroup childGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap server = new ServerBootstrap();
            // 1. 绑定两个线程组分别用来处理客户端通道的accept和读写时间
            server.group(parentGroup, childGroup)
                    // 2. 绑定服务端通道NioServerSocketChannel
                    .channel(NioServerSocketChannel.class)
                    // 3. 给读写事件的线程通道绑定handler去真正处理读写
                    // ChannelInitializer初始化通道SocketChannel
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            // 请求解码器
                            socketChannel.pipeline().addLast("decoder", new HttpRequestDecoder());
                            // 将HTTP消息的多个部分合成一条完整的HTTP消息最大长度100M
                            socketChannel.pipeline().addLast("aggregator", new HttpObjectAggregator(100 * 1024 * 1024));
                            // 响应转码器
                            socketChannel.pipeline().addLast("encoder", new HttpResponseEncoder());
                            // 解决大码流的问题，ChunkedWriteHandler：向客户端发送HTML5文件
                            socketChannel.pipeline().addLast("chunked", new ChunkedWriteHandler());
                            // 自定义处理handler
                            socketChannel.pipeline().addLast("handler", nettyHttpServerHandler);
                        }
                    });

            // 4. 监听端口（服务器host和port端口），同步返回
            ChannelFuture future = server.bind(CrispySettingConfig.SERVER_PORT).sync();
            LOGGER.info("crispy server start success，bind port：{}", CrispySettingConfig.SERVER_PORT);
            // 当通道关闭时继续向后执行，这是一个阻塞方法
            future.channel().closeFuture().sync();
        } finally {
            childGroup.shutdownGracefully();
            parentGroup.shutdownGracefully();
        }
    }


}