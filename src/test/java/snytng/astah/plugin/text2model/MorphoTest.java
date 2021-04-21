package snytng.astah.plugin.text2model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import snytng.astah.plugin.text2model.Morpho;

public class MorphoTest {

	@Test
	public void test単文() {
		String[][] output = Morpho.getSubObjVrb("車は道を走る");
		assertArrayEquals(output[0], new String[]{"車","道", null, "を走る"});
		assertThat(output.length, is(1));
}

	@Test
	public void testの() {
		String[][] output = Morpho.getSubObjVrb("車は道の真ん中を走る");
		assertArrayEquals(output[0], new String[]{"車","道", "真ん中", "を走る"});
		assertThat(output.length, is(1));
	}
	
	@Test
	public void test複数の文() {
		String[][] output = Morpho.getSubObjVrb("車は道を走る。車は道の真ん中を走る");
		assertArrayEquals(output[0], new String[]{"車","道", null, "を走る"});
		assertArrayEquals(output[1], new String[]{"車","道", "真ん中", "を走る"});
		assertThat(output.length, is(2));
	}
	

	@Test
	public void test一種() {
		String[][] output = Morpho.getSubObjVrb("バスは車の一種だ");
		assertArrayEquals(output[0], new String[]{"バス","車", "一種", "だ"});
		assertThat(output.length, is(1));
	}

	@Test
	public void test一部() {
		String[][] output = Morpho.getSubObjVrb("タイヤは車の一部だ");
		assertArrayEquals(output[0], new String[]{"タイヤ","車", "一部", "だ"});
		assertThat(output.length, is(1));
	}

	@Test
	public void test持つ() {
		String[][] output = Morpho.getSubObjVrb("車はタイヤを持つ");
		assertArrayEquals(output[0], new String[]{"車","タイヤ", null, "を持つ"});
		assertThat(output.length, is(1));
	}

	
	@Test
	public void test動詞変形し() {
		String[][] output = Morpho.getSubObjVrb("プリンタはジョブの印刷部数を取得し");
		assertArrayEquals(output[0], new String[]{"プリンタ","ジョブ", "印刷部数", "を取得する"});
		assertThat(output.length, is(1));
	}

	@Test
	public void test動詞変形して() {
		String[][] output = Morpho.getSubObjVrb("プリンタはジョブの印刷部数を取得して");
		assertArrayEquals(output[0], new String[]{"プリンタ","ジョブ", "印刷部数", "を取得する"});
		assertThat(output.length, is(1));
	}

	@Test
	public void test複文() {
		String[][] output = Morpho.getSubObjVrb("プリンタはジョブの印刷部数を取得して、同じタイプのプリンタを検索して、印刷部数を分割して、新しいジョブを転送する。");
		assertArrayEquals(output[0], new String[]{"プリンタ","ジョブ", "印刷部数", "を取得する"});
		assertArrayEquals(output[1], new String[]{"プリンタ","同じタイプ", "プリンタ", "を検索する"});
		assertArrayEquals(output[2], new String[]{"プリンタ","印刷部数", null, "を分割する"});
		assertArrayEquals(output[3], new String[]{"プリンタ","新しいジョブ", null, "を転送する"});
		assertThat(output.length, is(4));
	}

	@Test
	public void testである() {
		String[][] output = Morpho.getSubObjVrb("タイヤは車の一部である");
		assertArrayEquals(output[0], new String[]{"タイヤ","車", "一部", "だ"});
		assertThat(output.length, is(1));
	}

	@Test
	public void test主語並列() {
		String[][] output = Morpho.getSubObjVrb("タイヤとハンドルとウィンカーは車の一部である");
		assertArrayEquals(output[0], new String[]{"タイヤ","車", "一部", "だ"});
		assertArrayEquals(output[1], new String[]{"ハンドル","車", "一部", "だ"});
		assertArrayEquals(output[2], new String[]{"ウィンカー","車", "一部", "だ"});
		assertThat(output.length, is(3));
	}

	@Test
	public void test目的語並列() {
		String[][] output = Morpho.getSubObjVrb("車はタイヤとハンドルとウィンカーを持つ");
		assertArrayEquals(output[0], new String[]{"車","タイヤ", null, "を持つ"});
		assertArrayEquals(output[1], new String[]{"車","ハンドル", null, "を持つ"});
		assertArrayEquals(output[2], new String[]{"車","ウィンカー", null, "を持つ"});
		assertThat(output.length, is(3));
	}
	
	@Test
	public void test属性付き目的語並列() {
		String[][] output = Morpho.getSubObjVrb("パーツカタログはタイヤとハンドルの型番を持つ");
		assertArrayEquals(output[0], new String[]{"パーツカタログ","タイヤ", "型番", "を持つ"});
		assertArrayEquals(output[1], new String[]{"パーツカタログ","ハンドル", "型番", "を持つ"});
		assertThat(output.length, is(2));
	}
	
	@Test
	public void testから() {
		String[][] output = Morpho.getSubObjVrb("アプリは天気予報をネットから取得する");
		assertArrayEquals(output[0], new String[]{"アプリ","天気予報", null, "をネットから取得する"});
		assertThat(output.length, is(1));
	}

	@Test
	public void test修飾節あり() {
		String[][] output = Morpho.getSubObjVrb("アプリは、受信した電子メールをキーワードで検索し、");
		assertArrayEquals(output[0], new String[]{"アプリ","受信した電子メール", null, "をキーワードで検索する"});
		assertThat(output.length, is(1));
	}
	
	@Test
	public void testUSDM例文MAL01() {
		String[][] output = Morpho.getSubObjVrb("アプリは、事前に指定された受信および送信した電子メールをキーワードで検索し、選択した電子メールをメーラーに繋いで再利用したい");
		assertArrayEquals(output[0], new String[]{"アプリ","事前に指定された受信および送信した電子メール", null, "をキーワードで検索する"});
		assertArrayEquals(output[1], new String[]{"アプリ","選択した電子メール", null, "をメーラーに繋ぐ"});
		assertThat(output.length, is(2));
	}
	
	@Test
	public void testUSDM例文SAL01() {
		String[][] output = Morpho.getSubObjVrb("アプリは、警告メッセージをマネージャーのPC画面に出す");
		assertArrayEquals(output[0], new String[]{"アプリ","警告メッセージ", null, "をマネージャーのPC画面に出す"});
		assertThat(output.length, is(1));
	}

	@Test
	public void testUSDM例文SAL01_2() {
		String[][] output = Morpho.getSubObjVrb("アプリは、売上数量の予測データを商品ごとに設定し、売上数量の実売データを取得し、売上数量の予実差が大きいかどうかを確認し、予実差が大きいという警告メッセージをマネージャーのPC画面に出す");
		assertArrayEquals(output[0], new String[]{"アプリ","売上数量", "予測データ", "を商品ごとに設定する"});
		assertArrayEquals(output[1], new String[]{"アプリ", "売上数量", "実売データ", "を取得する"});
		assertArrayEquals(output[2], new String[]{"アプリ", "売上数量", "予実差が大きいかどうか", "を確認する"});
		assertArrayEquals(output[3], new String[]{"アプリ","予実差が大きいという警告メッセージ", null, "をマネージャーのPC画面に出す"});
		assertThat(output.length, is(4));
	}

	@Test
	public void test点点丸丸() {
		String[][] output = Morpho.getSubObjVrb("車は、、道を走る。。");
		assertArrayEquals(output[0], new String[]{"車","道", null, "を走る"});
		assertThat(output.length, is(1));
	}

	@Test
	public void test改行2() {
		String[][] output = Morpho.getSubObjVrb("車は、道を走る。\n\n");
		assertArrayEquals(output[0], new String[]{"車","道", null, "を走る"});
		assertThat(output.length, is(1));
	}

	@Test
	public void test改行4() {
		String[][] output = Morpho.getSubObjVrb("車は、道を走る。\n\n\n\n");
		assertArrayEquals(output[0], new String[]{"車","道", null, "を走る"});
		assertThat(output.length, is(1));
	}

	@Test
	public void test2文改行() {
		String[][] output = Morpho.getSubObjVrb("車は、道を走る\n車は、道を走る");
		assertArrayEquals(output[0], new String[]{"車","道", null, "を走る"});
		assertArrayEquals(output[1], new String[]{"車","道", null, "を走る"});
		assertThat(output.length, is(2));
	}

	@Test
	public void test2文改行2() {
		String[][] output = Morpho.getSubObjVrb("車は、道を走る\n\n車は、道を走る");
		assertArrayEquals(output[0], new String[]{"車","道", null, "を走る"});
		assertArrayEquals(output[1], new String[]{"車","道", null, "を走る"});
		assertThat(output.length, is(2));
	}

	
	@Test
	public void testをを() {
		String[][] output = Morpho.getSubObjVrb("車は、道路を砂浜を走る。");
		assertArrayEquals(output[0], new String[]{"車","道路", null, "を砂浜を走る"});
		assertThat(output.length, is(1));
	}

	@Test
	public void test主語なし() {
		String[][] output = Morpho.getSubObjVrb("道を走る");
		assertArrayEquals(output[0], new String[]{null,"道", null, "を走る"});
		assertThat(output.length, is(1));
	}

	@Test
	public void test目的語なし() {
		String[][] output = Morpho.getSubObjVrb("車はを走る");
		assertThat(output.length, is(0));
	}

	@Test
	public void testをなし() {
		String[][] output = Morpho.getSubObjVrb("車は走る");
		assertThat(output.length, is(0));
	}

	@Test
	public void test動詞なし() {
		String[][] output = Morpho.getSubObjVrb("車は道を");
		assertArrayEquals(output[0], new String[]{"車","道", null, null});
		assertThat(output.length, is(1));
	}

	@Test
	public void testだで目的語なし() {
		String[][] output = Morpho.getSubObjVrb("車をだ");
		assertThat(output.length, is(0));
	}

	@Test
	public void testであるで目的語なし() {
		String[][] output = Morpho.getSubObjVrb("車をである");
		assertThat(output.length, is(0));
	}
	
	@Test
	public void test空白あり() {
		String[][] output = Morpho.getSubObjVrb(" 車は道を走る");
		assertArrayEquals(output[0], new String[]{"車","道", null, "を走る"});
		assertThat(output.length, is(1));
	}

	@Test
	public void test空白あり途中() {
		String[][] output = Morpho.getSubObjVrb("車は道 を走る");
		assertArrayEquals(output[0], new String[]{"車","道", null, "を走る"});
		assertThat(output.length, is(1));
	}

	@Test
	public void testタブあり() {
		String[][] output = Morpho.getSubObjVrb("\t車は道を走る");
		assertArrayEquals(output[0], new String[]{"車","道", null, "を走る"});
		assertThat(output.length, is(1));
	}

	@Test
	public void testタブあり途中() {
		String[][] output = Morpho.getSubObjVrb("車は\t道を走る");
		assertArrayEquals(output[0], new String[]{"車","道", null, "を走る"});
		assertThat(output.length, is(1));
	}

	@Test
	public void test全角空白あり() {
		String[][] output = Morpho.getSubObjVrb("　車は道を走る");
		assertArrayEquals(output[0], new String[]{"車","道", null, "を走る"});
		assertThat(output.length, is(1));
	}
	
	@Test
	public void test全角空白あり途中() {
		String[][] output = Morpho.getSubObjVrb("車は道を　走る");
		assertArrayEquals(output[0], new String[]{"車","道", null, "を走る"});
		assertThat(output.length, is(1));
	}

	@Test
	public void testだで終わり主語を動詞で修飾() {
		String[][] output = Morpho.getSubObjVrb("巻き上げゲートはゲートの一種だ");
		assertArrayEquals(output[0], new String[]{"巻き上げゲート","ゲート", "一種", "だ"});
		assertThat(output.length, is(1));
	}

	@Test
	public void testだで終わり目的語を動詞で修飾() {
		String[][] output = Morpho.getSubObjVrb("ゲートは巻き上げゲートの一種だ");
		assertArrayEquals(output[0], new String[]{"ゲート","巻き上げゲート", "一種", "だ"});
		assertThat(output.length, is(1));
	}

	@Test
	public void testだで終わらなくて主語を動詞で修飾() {
		String[][] output = Morpho.getSubObjVrb("巻き上げゲートはゲートになる");
		assertThat(output.length, is(0));
	}

	@Test
	public void testだで終わらなくて目的を動詞で修飾() {
		String[][] output = Morpho.getSubObjVrb("ゲートは巻き上げゲートになる");
		assertThat(output.length, is(0));
	}
	
}
