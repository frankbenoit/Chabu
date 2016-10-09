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
using System;
using System.Diagnostics;

namespace Org.Chabu.Prot.V1
{

    public class ChabuException : SystemException {

	    private readonly int code;
        private readonly int remoteCode;
	
	    public ChabuException( String message )
            : base( message ) {

                Debug.WriteLine("ChabuException: " + message);
		    this.code = (int)ChabuErrorCode.UNKNOWN;
		    this.remoteCode = 0;
	    }

        public ChabuException(ChabuErrorCode error, String message)
            : base(message)
        {

            this.code = (int)error;
		    this.remoteCode = 0;
	    }

        public ChabuException(ChabuErrorCode error, int remoteCode, String message)
            : base(message)
        {

            this.code = (int)error;
		    this.remoteCode = remoteCode;
	    }
	
	    public ChabuException( int code, String message )        : base( message ) {

		    this.code = code;
		    this.remoteCode = 0;
	    }
	
	    public int getCode() {
		    return code;
	    }
	    public int getRemoteCode() {
		    return remoteCode;
	    }
    }
}