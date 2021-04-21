package snytng.astah.plugin.text2model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import snytng.astah.plugin.text2model.Morpho;

public class MorphoNounsTest {

	@Test
	public void test単文() {
		String[][] output = Morpho.getNouns("自動車は道路を走る");
		assertThat(output.length, is(1));
		assertArrayEquals(output[0], new String[]{"自動車","道路", null, null});
	}

	@Test
	public void test複文() {
		String[][] output = Morpho.getNouns("車は道路を走り、帆船は海洋を進む");
		assertThat(output.length, is(6));
		assertArrayEquals(output[0], new String[]{"車","道路", null, null});
		assertArrayEquals(output[1], new String[]{"車","帆船", null, null});
		assertArrayEquals(output[2], new String[]{"車","海洋", null, null});
		assertArrayEquals(output[3], new String[]{"道路","帆船", null, null});
		assertArrayEquals(output[4], new String[]{"道路","海洋", null, null});
		assertArrayEquals(output[5], new String[]{"帆船","海洋", null, null});
	}

	@Test
	public void testストップワードtrue() { // stopword=道
		String[][] output = Morpho.getNouns("車は道と道路", true, true);
		assertThat(output.length, is(1));
	}

	@Test
	public void testストップワードfalse() { // stopword=道
		String[][] output = Morpho.getNouns("車は道と道路", true, false);
		assertThat(output.length, is(3));
	}

	@Test
	public void testサ変接続true() {
		String[][] output = Morpho.getNouns("車はタクシーは変化", true, true);
		assertThat(output.length, is(3));
	}

	@Test
	public void testサ変false() {
		String[][] output = Morpho.getNouns("車はタクシーは変化", false, true);
		assertThat(output.length, is(1));
	}

	
}
