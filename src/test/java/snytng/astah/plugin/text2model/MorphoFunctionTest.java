package snytng.astah.plugin.text2model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import snytng.astah.plugin.text2model.Morpho;

public class MorphoFunctionTest {

	// test getSubFuntion
	@Test
	public void test単文() {
		String[][] output = Morpho.getSubFunction("自動車は走る");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{"自動車",null, null,"走る"});
	}

	@Test
	public void test単文主語なし() {
		String[][] output = Morpho.getSubFunction("走る");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{null,null, null,"走る"});
	}

	@Test
	public void test単文目的語() {
		String[][] output = Morpho.getSubFunction("自動車は道路を走る");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{"自動車",null, null,"道路を走る"});
	}

	@Test
	public void test複文() {
		String[][] output = Morpho.getSubFunction("車は道路を走り、帆船は海洋を進む");
		assertThat(output.length, is(2));
		assertArrayEquals(output[0], new String[]{"車",null, null, "道路を走る"});
		assertArrayEquals(output[1], new String[]{"帆船",null, null, "海洋を進む"});
	}
	
	@Test
	public void test複文句読点() {
		String[][] output = Morpho.getSubFunction("車は、道路を走り、帆船は、海洋を進む");
		assertThat(output.length, is(2));
		assertArrayEquals(output[0], new String[]{"車",null, null, "道路を走る"});
		assertArrayEquals(output[1], new String[]{"帆船",null, null, "海洋を進む"});
	}
	
	@Test
	public void test主語が一つで複文() {
		String[][] output = Morpho.getSubFunction("車は道路を走り、車庫に入る");
		assertThat(output.length, is(2));
		assertArrayEquals(output[0], new String[]{"車",null, null, "道路を走る"});
		assertArrayEquals(output[1], new String[]{"車",null, null, "車庫に入る"});
	}

	// test getSubFuntions
	@Test
	public void testGetSubFunctions単文() {
		String[][] output = Morpho.getSubFunctions("自動車は走る");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{"自動車",null, null,"走る"});
	}

	@Test
	public void testGetSubFunctions単文主語なし() {
		String[][] output = Morpho.getSubFunctions("走る");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{null,null, null,"走る"});
	}

	@Test
	public void testGetSubFunctions単文目的語() {
		String[][] output = Morpho.getSubFunctions("自動車は道路を走る");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{"自動車",null, null,"道路を走る"});
	}

	@Test
	public void testGetSubFunctions複文() {
		String[][] output = Morpho.getSubFunctions("車は道路を走り、帆船は海洋を進む");
		assertThat(output.length, is(2));
		assertArrayEquals(output[0], new String[]{"車",null, null, "道路を走る"});
		assertArrayEquals(output[1], new String[]{"帆船",null, null, "海洋を進む"});
	}
	
	@Test
	public void testGetSubFunctions複文句読点() {
		String[][] output = Morpho.getSubFunction("車は、道路を走り、帆船は、海洋を進む");
		assertThat(output.length, is(2));
		assertArrayEquals(output[0], new String[]{"車",null, null, "道路を走る"});
		assertArrayEquals(output[1], new String[]{"帆船",null, null, "海洋を進む"});
	}
	
	@Test
	public void testGetSubFunctions主語が一つで複文() {
		String[][] output = Morpho.getSubFunctions("車は道路を走り、車庫に入る");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{"車",null, null, "道路を走る>車庫に入る"});
	}

	@Test
	public void testGetSubFunctions主語が一つで複文include必ず() {
		String[][] output = Morpho.getSubFunctions("運転手は車を運転するとき、必ずハンドルを握る");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{"運転手",null, null, "車を運転する<I>ハンドルを握る"});
	}

	@Test
	public void testGetSubFunctions主語が一つで複文includeいつも() {
		String[][] output = Morpho.getSubFunctions("運転手は車を運転するとき、いつもハンドルを握る");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{"運転手",null, null, "車を運転する<I>ハンドルを握る"});
	}

	@Test
	public void testGetSubFunctions主語が一つで複文extend() {
		String[][] output = Morpho.getSubFunctions("運転手は車を運転するとき、ハンドルを握る");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{"運転手",null, null, "車を運転する<E>ハンドルを握る"});
	}

	
}
