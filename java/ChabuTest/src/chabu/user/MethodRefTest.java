package chabu.user;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Test;

public class MethodRefTest {
	
	Random rnd = new Random();
	static final String str = "";
	public class C {
		long act1( String s ){
			return 0;//rnd.nextLong();
		}
		void act2( String s ){
		}
	}
	
	BiFunction<C, String, Long> f1 = new BiFunction<C, String, Long>() {
		public Long apply(C t, String u) {
			return t.act1(u);
		}
	};
	
	BiConsumer<C, String> f2 = new BiConsumer<C, String>() {
		public void accept(C t, String u) {
			t.act2(u);
		}
	};
	
	private void func1( C c, BiFunction<C, String, Long> f ){
		@SuppressWarnings("unused")
		long l = f.apply(c, str);
	}
	private void func2( Function<String, Long> f ){
		@SuppressWarnings("unused")
		long l = f.apply(str);
	}
	private void cons1( C c, BiConsumer<C, String> f ){
		f.accept( c, str);
	}
	private void cons2( Consumer<String> f ){
		f.accept( str);
	}
	
	private C c;

	@Test
	public void testFunc() {
		c = new C();
		System.nanoTime();
		long s0 = System.nanoTime();
		func1( c, f1 );
		func1( c, C::act1 );
		func2( c::act1 );
		cons1( c, f2 );
		cons1( c, C::act2 );
		cons2( c::act2 );
		long s1 = System.nanoTime();
		System.out.printf("Duration first call %14.3f ms\n", ( s1 - s0 ) * 1e-6 );

		testRun(           1 );
		testRun(       1_000 );
		testRun(   1_000_000 );
		testRun(  10_000_000 );
	}
	public void testRun( int loops) {
		
		
		long s0 = System.nanoTime();
		long s1 = System.nanoTime();
		for( int i = 0; i < loops; i++ ){
			func1( c, f1 );
		}
		long s2 = System.nanoTime();
		for( int i = 0; i < loops; i++ ){
			func1( c, C::act1 );
		}
		long s3 = System.nanoTime();
		for( int i = 0; i < loops; i++ ){
			func2( c::act1 );
		}
		long s4 = System.nanoTime();
		for( int i = 0; i < loops; i++ ){
			c.act1("");
		}
		long s5 = System.nanoTime();
		for( int i = 0; i < loops; i++ ){
			cons1( c, f2 );
		}
		long s6 = System.nanoTime();
		for( int i = 0; i < loops; i++ ){
			cons1( c, C::act2 );
		}
		long s7 = System.nanoTime();
		for( int i = 0; i < loops; i++ ){
			cons2( c::act2 );
		}
		long s8 = System.nanoTime();
		for( int i = 0; i < loops; i++ ){
			c.act2("");
		}
		long s9 = System.nanoTime();
		long d = s1 - s0;
		
		int e = (int)Math.log10(loops);
		int dig = (int)( loops / ( Math.pow(10, e)));
		System.out.printf("--- loops %de%d -------------- Function -×------ Consumer -×\n", dig, e );
		System.out.printf("Step 1, interface    : %12.1f ns | %12.1f ns |\n", ( s2 - s1 -d ) / (double)loops, ( s6 - s5 -d ) / (double)loops );
		System.out.printf("Step 2, class mth    : %12.1f ns | %12.1f ns |\n", ( s3 - s2 -d ) / (double)loops, ( s7 - s6 -d ) / (double)loops );
		System.out.printf("Step 3, object mth   : %12.1f ns | %12.1f ns |\n", ( s4 - s3 -d ) / (double)loops, ( s8 - s7 -d ) / (double)loops );
		System.out.printf("Simple call          : %12.1f ns | %12.1f ns |\n", ( s5 - s4 -d ) / (double)loops, ( s9 - s8 -d ) / (double)loops );
		System.out.printf("Duration %14.3f ms\n", ( s9 - s1 ) * 1e-6 );
	}
	
}
