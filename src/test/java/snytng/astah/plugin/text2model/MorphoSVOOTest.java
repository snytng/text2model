package snytng.astah.plugin.text2model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import snytng.astah.plugin.text2model.Morpho;

public class MorphoSVOOTest {

	@Test
	public void test単文() {
		String[][] output = Morpho.getSVOO("停止中は、起動イベントで起動中になる。");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{"停止中", "起動イベント", null, "起動中"});
	}

	@Test
	public void test単文動詞変化影響なし() {
		String[][] output = Morpho.getSVOO("停止中は、起動イベントで起動中に遷移する。");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{"停止中", "起動イベント", null, "起動中"});
	}

	@Test
	public void test単文主語なし() {
		String[][] output = Morpho.getSVOO("起動イベントで起動中になる。");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{null, "起動イベント", null, "起動中"});
	}

	@Test
	public void test単文目的語() {
		String[][] output = Morpho.getSVOO("停止中は、起動中になる。");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{"停止中", null, null, "起動中"});
	}

	@Test
	public void test複文() {
		String[][] output = Morpho.getSVOO("停止中は、起動イベントで起動中に遷移する。停止中は、起動イベントで起動中に遷移する。");
		assertThat(output.length, is(2));
		assertArrayEquals(output[0], new String[]{"停止中", "起動イベント", null, "起動中"});
		assertArrayEquals(output[1], new String[]{"停止中", "起動イベント", null, "起動中"});
	}
	
	@Test
	public void test複文句読点() {
		String[][] output = Morpho.getSVOO("停止中は、起動イベントで起動中に遷移し、停止中は、起動イベントで起動中に遷移する。");
		assertThat(output.length, is(2));
		assertArrayEquals(output[0], new String[]{"停止中", "起動イベント", null, "起動中"});
		assertArrayEquals(output[1], new String[]{"停止中", "起動イベント", null, "起動中"});
	}
	
	@Test
	public void test主語が一つで複文() {
		String[][] output = Morpho.getSVOO("停止中は、起動イベントで起動中に遷移し、一時停止イベントで一時停止中に遷移する。");
		assertThat(output.length, is(2));
		assertArrayEquals(output[0], new String[]{"停止中", "起動イベント", null, "起動中"});
		assertArrayEquals(output[1], new String[]{"停止中", "一時停止イベント", null, "一時停止中"});
	}
}
