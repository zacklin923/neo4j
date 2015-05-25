/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.ndp.transport.socket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.ByteBuffer;

import org.neo4j.kernel.impl.util.HexPrinter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChunkedOutputTest
{
    private final Channel ch = mock( Channel.class );
    private final ByteBuffer writtenData = ByteBuffer.allocate( 1024 );
    private ChunkedOutput out ;

    @Test
    public void shouldChunkSingleMessage() throws Throwable
    {
        // When
        out.writeByte( (byte) 1 ).writeShort( (short) 2 );
        out.messageBoundaryHook().run();
        out.flush();

        // Then
        assertThat( writtenData.limit(), equalTo( 7 ) );
        assertThat( HexPrinter.hex( writtenData, 0, 7 ),
                equalTo( "00 03 01 00 02 00 00" ) );
    }

    @Test
    public void shouldChunkMessageSpanningMultipleChunks() throws Throwable
    {
        // When
        out.writeLong( 1 ).writeLong( 2 ).writeLong( 3 );
        out.messageBoundaryHook().run();
        out.flush();

        // Then
        assertThat( writtenData.limit(), equalTo( 32 ) );
        assertThat( HexPrinter.hex( writtenData, 0, 32 ),
                equalTo( "00 08 00 00 00 00 00 00    00 01 00 08 00 00 00 00    " +
                         "00 00 00 02 00 08 00 00    00 00 00 00 00 03 00 00" ) );
    }

    @Before
    public void setup()
    {
        when( ch.alloc() ).thenReturn( UnpooledByteBufAllocator.DEFAULT );
        when( ch.writeAndFlush( any(), any( ChannelPromise.class ) ) ).thenAnswer( new Answer<Object>()
        {
            @Override
            public Object answer( InvocationOnMock invocation ) throws Throwable
            {
                ByteBuf byteBuf = (ByteBuf) invocation.getArguments()[0];
                writtenData.limit( writtenData.position() + byteBuf.readableBytes() );
                byteBuf.readBytes( writtenData );
                return null;
            }
        } );
        this.out = new ChunkedOutput( ch, 16 );
    }

    @After
    public void teardown()
    {
        out.close();
    }

}