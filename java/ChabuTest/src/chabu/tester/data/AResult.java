package chabu.tester.data;

import java.nio.ByteBuffer;

import chabu.Utils;

public abstract class AResult extends AXferItem {
	
	public final ResultId resultId;
	public final long time;
	
	
	AResult( ResultId resultId, long time ){
		this.resultId = resultId;
		this.time = time;
	}

	@Override
	public void encode(ByteBuffer buf) {
		buf.put( (byte)this.resultId.getId() );
		buf.putLong( this.time );
	}

	public static AResult decodeResult(ByteBuffer buf) {
		//System.out.printf("ACommand.decode %s\n", buf);
		if( buf.remaining() < 4 ) return null;
		
		int pos = buf.position();
		int packetSz = buf.getInt();
		
		//System.out.printf("ACommand.decode pos:%s sz:%s\n", pos, packetSz);
		if( buf.remaining() < packetSz ){
			buf.position(pos);
			return null;
		}
		
		AResult res = null;
		try{
			int resId = buf.get();
			if( resId == ResultId.VERSION.getId() ){
				res = ResultVersion.createResultChannelStat(buf);
			}
			else if( resId == ResultId.ERROR.getId() ){
				res = ResultError.createResultChannelStat(buf);
			}
			else if( resId == ResultId.CHANNEL_STAT.getId() ){
				res = ResultChannelStat.createResultChannelStat(buf);
			}
			else {
				throw Utils.fail( "unknown result id %d", resId );
			}
		}
		finally{
			if( res != null ){
				Utils.ensure( pos + packetSz + 4 == buf.position(), "%d %d +4 == %s", pos, packetSz, buf );
			}
			else {
				buf.position(pos);
			}
		}
		return res;
	}

}
