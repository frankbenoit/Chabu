/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit, Stuttgart, Germany <keinfarbton@gmail.com>
 *
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 *
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
package org.chabu.prot.v1.internal;

import java.nio.ByteBuffer;

/**
 * A replacement for Java8 <code>Consumer&lt;ByteBuffer&gt;</code>
 *
 * @author Frank Benoit
 */
public interface ConsumerByteBuffer {

    public void accept( ByteBuffer buffer );

}
