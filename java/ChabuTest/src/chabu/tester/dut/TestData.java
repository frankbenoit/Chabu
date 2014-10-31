package chabu.tester.dut;

public class TestData {

	private static final int SEED = 5323;
	private static final int SIZE = 0x1000;
	private final byte[] data;
	
	public TestData(){
		Tausworthe rnd = new Tausworthe(SEED);
		data = new byte[ SIZE ];
		rnd.nextBytes( data );
	}
	
	public byte get( int index ){
		return data[ index % SIZE ];
	}
	
	public static void main(String[] args) {
		TestData td = new TestData();
		for( int i = 0; i < SIZE; i += 16 ){
			for( int k = 0; k < 16; k ++ ){
				System.out.printf("%02X ", td.get( i + k ) & 0xFF );
			}
			System.out.println();
		}
	}
	
	
}
