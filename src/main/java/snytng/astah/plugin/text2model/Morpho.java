package snytng.astah.plugin.text2model;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.atilika.kuromoji.Token;
import org.atilika.kuromoji.Tokenizer;

public class Morpho {

	private Morpho(){}

	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(Morpho.class.getName());
	static {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.CONFIG);      
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);
	}

	/**
	 * Kuromoji Tokenizer 初期化
	 */
	private static Tokenizer ktokenizer = Tokenizer.builder().build();

	/**
	 * kuromojiで主語、目的語、動詞の配列を作成する
	 * @param str 文字列。句読点、改行を含む。主語を必ず一つ含む。
	 * @return 主語、目的語、動詞の配列
	 */
	public static String[][] getSubObjVrb(String str) {

		String[] sentences = getSentences(str);

		// 主語、目的語、属性、動詞の4つに分解
		List<String[]> ret = new ArrayList<>();

		for(int i = 0; i < sentences.length; i++){
			String sentence = sentences[i];

			List<Token> ktokens = ktokenizer.tokenize(sentence);

			// 目的語の存在確認
			boolean hasObject = false;
			for(Token t : ktokens){
				String f0 = t.getAllFeaturesArray()[0];
				String f1 = t.getAllFeaturesArray()[1];
				String f6 = t.getAllFeaturesArray()[6]; // 原形

				if(f0.equals("助詞") && f1.equals("格助詞") && f6.equals("を")){ // 文に目的語＝「を」あり
					hasObject = true;
					break;
				}
			}

			//「だ」文かどうかの確認
			boolean hasDa = false;
			for(Token t : ktokens){
				String f0 = t.getAllFeaturesArray()[0];
				String f6 = t.getAllFeaturesArray()[6]; // 原形

				if(f0.equals("助動詞") && f6.equals("だ")){ // 文に「だ」あり
					hasDa = true;
					break;
				}
			}


			StringBuilder tmp = new StringBuilder("");
			String sub = "";
			String obj = "";
			String vrb = "";
			for(Token t : ktokens){
				logger.info(t.getSurfaceForm() + " " + t.getAllFeatures());

				String f0 = t.getAllFeaturesArray()[0];
				String f1 = t.getAllFeaturesArray()[1];
				String f6 = t.getAllFeaturesArray()[6]; // 原形

				if(f0.equals("助詞") && f1.equals("係助詞")){ // 主語
					sub = tmp.toString();
					tmp = new StringBuilder("");

				} else if(f0.equals("助詞") && f1.equals("格助詞") && f6.equals("を")){ // 目的語　「を」で分割
					if(obj.equals("")){
						obj = tmp.toString();
						tmp = new StringBuilder(t.getSurfaceForm());
					} else {
						tmp.append(t.getSurfaceForm());
					}

				} else if(f0.equals("助詞") && f1.equals("接続助詞")){ // 接続助詞は読み捨てる

				} else if(f0.equals("動詞")){ // 助詞＋動詞
					if(hasObject && obj.equals("")){ // 目的語があり取得前だったら、次に接続させる
						tmp.append(t.getSurfaceForm());
					} else if(hasDa && (sub.equals("") || obj.equals(""))){ // 「だ」で終わり、主語・目的語を取得前だったら、次に接続させる
						tmp.append(t.getSurfaceForm());
					} else {
						tmp.append(f6); // 原形を採用する
						vrb = tmp.toString();
						register(ret, sub, obj, vrb);

						tmp = new StringBuilder("");
						obj = "";
					}

				} else if(f0.equals("助動詞")){ // 助動詞
					if(hasObject){
						tmp.append(t.getSurfaceForm());
					} else{
						obj = tmp.toString();
						if(!obj.isEmpty()){
							vrb = f6; // 原形を採用する
							register(ret, sub, obj, vrb);
						}

						tmp = new StringBuilder("");
						obj = "";
					}

				} else { // それ以外は次に接続
					tmp.append(t.getSurfaceForm());
				}
			} // ktokens

			// 動詞・助動詞のみがない場合にvrbをnullにして登録
			if(! sub.equals("") && ! obj.equals("") && vrb.equals("")){
				register(ret, sub, obj, null);
			}

		}

		return ret.toArray(new String[][]{});
	}

	private static void register(List<String[]> ret, String sub, String obj, String vrb) {
		StringBuilder tmp = new StringBuilder("");

		// 目的語から属性を分割　「水の温度」→obj=水、attr=温度
		String attr = null;
		boolean hasAttr = false;

		List<Token> kobj = ktokenizer.tokenize(obj);
		for(Token kt : kobj){
			String h1 = kt.getAllFeaturesArray()[0];
			String h2 = kt.getAllFeaturesArray()[1];

			if(h1.equals("助詞") && h2.equals("連体化")){ // 「の」
				obj = tmp.toString();
				hasAttr = true;
				tmp = new StringBuilder("");
			} else { // それ以外は接続
				tmp.append(kt.getSurfaceForm());
			}
		}
		if(hasAttr){
			attr = tmp.toString();
		}

		// 主語を「と」で分割しながら複数エントリーを作る
		for(String subx : getMulti(sub)){
			// 目的語を「と」で分割しながら複数エントリーを作る
			for(String objx : getMulti(obj)){
				if(objx != null){
					String objstr = hasAttr ? objx + "の" + attr : objx;
					String logstr = subx + "," + objstr + "," + vrb;
					logger.info(logstr);
					ret.add(new String[]{subx, objx, attr, vrb});
				}
			}
		}
	}

	/**
	 * kuromojiで主語とそれ以外に分ける
	 * @param str 文字列。句読点、改行を含む。主語を必ず一つ含む。
	 * @return 主語とそれ以外の配列
	 */
	public static String[][] getSubFunction(String str) {

		String[] sentences = getSentences(str);

		// 主語とそれ以外に分解
		List<String[]> ret = new ArrayList<>();

		for(int i = 0; i < sentences.length; i++){
			String sentence = sentences[i];

			List<Token> ktokens = ktokenizer.tokenize(sentence);


			StringBuilder tmp = new StringBuilder("");
			String sub = "";
			String obj = "";
			String vrb = "";
			for(Token t : ktokens){
				logger.info(t.getSurfaceForm() + " " + t.getAllFeatures());

				String f0 = t.getAllFeaturesArray()[0];
				String f1 = t.getAllFeaturesArray()[1];
				String f6 = t.getAllFeaturesArray()[6]; // 原形

				if(f0.equals("助詞") && f1.equals("係助詞")){ // 主語
					sub = tmp.toString();
					tmp = new StringBuilder("");

				} else if(f0.equals("動詞") || f0.equals("助動詞")){ 
					tmp.append(f6); // 原形を採用する
					vrb = tmp.toString();
					registerFunction(ret, sub, obj, vrb);

					tmp = new StringBuilder("");
					obj = "";

				} else { // それ以外は次に接続
					tmp.append(t.getSurfaceForm());
				}
			} // ktokens

			// 動詞・助動詞のみがない場合にvrbをnullにして登録
			if(! sub.equals("") && ! obj.equals("") && vrb.equals("")){
				registerFunction(ret, sub, obj, null);
			}

		}

		return ret.toArray(new String[][]{});
	}
	
	/**
	 * kuromojiで主語とそれ以外に分ける
	 * @param str 文字列。句読点、改行を含む。主語を必ず一つ含む。
	 * @return 主語とそれ以外の配列、複数機能をそのまま付けて返却する
	 */
	public static String[][] getSubFunctions(String str) {

		String[] sentences = getSentences(str);

		// 主語とそれ以外に分解
		List<String[]> ret = new ArrayList<>();

		for(int i = 0; i < sentences.length; i++){
			String sentence = sentences[i];

			List<Token> ktokens = ktokenizer.tokenize(sentence);


			StringBuilder tmp = new StringBuilder("");
			String sub = "";
			StringBuilder vrb = new StringBuilder("");
			for(Token t : ktokens){
				logger.info(t.getSurfaceForm() + " " + t.getAllFeatures());

				String f0 = t.getAllFeaturesArray()[0];
				String f1 = t.getAllFeaturesArray()[1];
				String f6 = t.getAllFeaturesArray()[6]; // 原形

				if(f0.equals("助詞") && f1.equals("係助詞") && f6.equals("は")){ // 主語
					if(!sub.isEmpty()){
						if(vrb.toString().isEmpty()){
							registerFunction(ret, sub, "", null);
						} else {
							registerFunction(ret, sub, "", vrb.toString());
						}
					}
					sub = tmp.toString();
					vrb = new StringBuilder("");
					tmp = new StringBuilder("");

				} else if(f0.equals("動詞") || f0.equals("助動詞")){ 
					if(vrb.toString().endsWith("<")){
						vrb.append("E"); // <<extend>>
					}
					
					if(!vrb.toString().isEmpty()){
						vrb.append(">");
					}

					tmp.append(f6); // 原形を採用する
					vrb.append(tmp.toString());
					
					tmp = new StringBuilder("");

				} else if(f0.equals("名詞") && f6.equals("とき")){ 
					tmp.append("<");
					vrb.append(tmp.toString());
					tmp = new StringBuilder("");

				} else if(f0.equals("副詞") && (f6.equals("いつも") || f6.equals("必ず"))){ 
					tmp.append("I"); // <<include>>
					vrb.append(tmp.toString());
					tmp = new StringBuilder("");

				} else { // それ以外は次に接続
					tmp.append(t.getSurfaceForm());
				}
			} // ktokens

			// 動詞・助動詞のみがない場合にvrbをnullにして登録
			if(! sub.equals("") && vrb.toString().equals("")){
				registerFunction(ret, sub, "", null);
			} else {
				registerFunction(ret, sub, "", vrb.toString());				
			}

		}

		return ret.toArray(new String[][]{});
	}

	/**
	 * kuromojiで主語と間接目的語、直接目的語と動詞を取得する
	 * @param str 文字列。句読点、改行を含む。主語を必ず一つ含む。
	 * @return 主語と間接目的語、直接目的語と動詞の配列
	 */
	public static String[][] getSVOO(String str) {

		String[] sentences = getSentences(str);

		// 主語、目的語、属性、動詞の4つに分解
		List<String[]> ret = new ArrayList<>();

		for(int i = 0; i < sentences.length; i++){
			String sentence = sentences[i];

			List<Token> ktokens = ktokenizer.tokenize(sentence);

			StringBuilder tmp = new StringBuilder("");
			String sub = "";
			String obj = "";
			String obj2 = "";
			String vrb = "";
			for(Token t : ktokens){
				logger.info(t.getSurfaceForm() + " " + t.getAllFeatures());

				String f0 = t.getAllFeaturesArray()[0];
				String f1 = t.getAllFeaturesArray()[1];
				String f6 = t.getAllFeaturesArray()[6]; // 原形

				if(f0.equals("助詞") && f1.equals("係助詞") && f6.equals("は")){ // 主語
					sub = tmp.toString();
					tmp = new StringBuilder("");
					logger.info("主語=" + sub);

				} else if(f0.equals("助詞") && f1.equals("格助詞") && f6.equals("で")){ // 目的語　「で」で分割
					if(obj.equals("")){
						obj = tmp.toString();
						tmp = new StringBuilder("");
						logger.info("目的語=" + obj);
					} else {
						tmp.append(t.getSurfaceForm());
					}

				} else if(f0.equals("助詞") && f1.equals("格助詞") && f6.equals("に")){ // 目的語　「に」で分割
					if(obj2.equals("")){
						obj2 = tmp.toString();
						tmp = new StringBuilder(t.getSurfaceForm());
						logger.info("目的語2=" + obj2);
					} else {
						tmp.append(t.getSurfaceForm());
					}

				} else if(f0.equals("助詞") && f1.equals("接続助詞")){ // 接続助詞は読み捨てる

				} else if(f0.equals("動詞")){ // 助詞＋動詞
					tmp.append(f6); // 原形を採用する
					vrb = tmp.toString();
					logger.info("動詞=" + vrb);

					registerFunction(ret, sub, obj, obj2);

					tmp = new StringBuilder("");
					obj = "";
					obj2 = "";

				} else { // それ以外は次に接続
					tmp.append(t.getSurfaceForm());
				}
			} // ktokens
		} // sentences

		return ret.toArray(new String[][]{});
	}	
	
	// ユーティリティ関数

	private static String[] getSentences(String str) {
		// 空白をなくし、句点をなくし、読点をなくして改行に変換
		String kuromojistr = str
				.replaceAll("[ \\t\\r　]", "")
				.replaceAll("、", "")
				.replaceAll("。", "\n");

		// 改行で区切って文章ごとに文字列を作成
		return kuromojistr.split("\n");
	}

	private static void registerFunction(List<String[]> ret, String sub, String obj, String vrb) {
		StringBuilder tmp = new StringBuilder("");

		// 目的語から属性を分割　「水の温度」→obj=水、attr=温度
		String attr = null;
		boolean hasAttr = false;

		List<Token> kobj = ktokenizer.tokenize(obj);
		for(Token kt : kobj){
			String h1 = kt.getAllFeaturesArray()[0];
			String h2 = kt.getAllFeaturesArray()[1];

			if(h1.equals("助詞") && h2.equals("連体化")){ // 「の」
				obj = tmp.toString();
				hasAttr = true;
				tmp = new StringBuilder("");
			} else { // それ以外は接続
				tmp.append(kt.getSurfaceForm());
			}
		}
		if(hasAttr){
			attr = tmp.toString();
		}

		// 主語を「と」で分割しながら複数エントリーを作る
		for(String subx : getMulti(sub)){
			// 目的語を「と」で分割しながら複数エントリーを作る
			for(String objx : getMulti(obj)){
				String objstr = hasAttr ? objx + "の" + attr : objx;
				String logstr = subx + "," + objstr + "," + vrb;
				logger.info(logstr);
				ret.add(new String[]{subx, objx, attr, vrb});
			}
		}
	}

	private static List<String> getMulti(String str){
		List<String> list = new ArrayList<>();

		if(str == null || str.isEmpty()){
			list.add((String)null);
			return list;
		}

		StringBuilder tmp = new StringBuilder("");

		// 並列助詞を取得する
		List<Token> ksub = ktokenizer.tokenize(str);

		for(Token kt : ksub){
			String h1 = kt.getAllFeaturesArray()[0];
			String h2 = kt.getAllFeaturesArray()[1];

			if(h1.equals("助詞") && h2.equals("並立助詞")){ // 「と」
				list.add(tmp.toString());
				tmp = new StringBuilder("");
			} else { // それ以外は接続
				tmp.append(kt.getSurfaceForm());
			}
		}
		if(! tmp.toString().equals("")){
			list.add(tmp.toString());
		}

		return list;
	}

	public static String[][] getNouns(String str) {
		return getNouns(str, true, true);
	}

	public static String[][] getNouns(String str, boolean bsahen, boolean bstopwords) {

		String[] sentences = getSentences(str);

		// 名詞を抽出し、主語、目的語、属性として列挙
		List<String[]> ret = new ArrayList<>();

		for(int i = 0; i < sentences.length; i++){
			String sentence = sentences[i];

			List<Token> ktokens = ktokenizer.tokenize(sentence);

			Set<String> nounSet = new LinkedHashSet<>();
			for(Token t : ktokens){
				logger.info(t.getSurfaceForm() + " " + t.getAllFeatures());

				String f0 = t.getAllFeaturesArray()[0];
				String f1 = t.getAllFeaturesArray()[1];

				if(f0.equals("名詞") 
						&& (f1.equals("一般") || (bsahen && f1.equals("サ変接続")))){
					String noun = t.getSurfaceForm();

					if(bstopwords){
						if(! stopwords.contains(noun)){
							nounSet.add(noun);
						} else {
							logger.info(() -> noun + " is in stopwords!");
						}
					} else {
						nounSet.add(noun);						
					}
				}
			} // ktokens

			List<String> nounList = new ArrayList<>(nounSet);
			int nounListLen = nounList.size();
			for(int j = 0; j < nounListLen; j++){
				String n1 = nounList.get(j);
				for(int k = j+1; k < nounListLen; k++){
					String n2 = nounList.get(k);
					register(ret, n1, n2, null);
				}
			}

		} // sentences

		return ret.toArray(new String[][]{});
	}

	static Set<String> stopwords = new HashSet<>();
	static {

		try (
				InputStream is = Morpho.class.getResourceAsStream("stopwords.txt");
				Scanner scan = new Scanner(is, "UTF-8");
				) {
			
			while(scan.hasNext()){
				String str = scan.nextLine();
				if(str == null) {
					break;
				}
				stopwords.add(str);
				logger.finest(() -> "stopwords: " + str + " added");
			}
		} catch (Exception e) {
			logger.warning(e.getMessage());
		}
	}

}
