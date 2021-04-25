package snytng.astah.plugin.text2model;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.change_vision.jude.api.inf.editor.TransactionManager;
import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IClassDiagram;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.presentation.IPresentation;

public class ClassDiagramWriter implements DiagramWriter {

	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(ClassDiagramWriter.class.getName());
	static {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.CONFIG);
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);
	}

	private ClassDiagramWriter() {
	}

	private static ClassDiagramWriter instance = null;

	public static ClassDiagramWriter getInstance(){
		if(instance == null){
			instance = new ClassDiagramWriter();
		}
		return instance;
	}

	public Supplier<String> getModeGetter(){
		return () -> "クラス図モード | 属性：～の～、操作：～を～する、集約：～の一部だ、継承：～の一種だ、実現：～の実現だ、依存：～を使う";
	}

	private boolean nounMode = false;
	public void setNounMode(boolean b){
		this.nounMode = b;
	}

	public Function<String, String[][]> getTextAanalyzer(){
		return str -> {
			// 名詞モードの場合には、名詞ペアを抽出する
			if(nounMode){
				return Morpho.getNouns(str, /*サ変*/false, /*ストップワード*/true);
			}
			// それ以外は、主語、目的語、述語に分ける
			else {
				return Morpho.getSubObjVrb(str);
			}
		};
	}

	private boolean sequenceDiagramMode = false;
	public void setSequenceDiagramMode(boolean b){
		this.sequenceDiagramMode = b;
	}

	public Consumer<FunctionCreator> getFunctionVisualizer(){
		return f -> {
			if(f.subjectName.isEmpty() || f.objectName.isEmpty()){
				logger.log(Level.WARNING, "cannot add function because of argument(s) is empty.");
				return;
			}

			try {
				TransactionManager.beginTransaction();

				// 作成前ログを出力
				f.logName();

				IClassDiagram classDiagram  = (IClassDiagram)f.diagram;

				// ----- 主語と目的語
				for(IPresentation p : classDiagram.getPresentations()){
					f.setSubjectOrObject(p);
				}

				// 主語のクラスがなければ図に追加
				f.addSubject(getNewClassPosition(classDiagram));

				// 目的語のクラスがなければ図に追加
				f.addObject(getNewClassPosition(classDiagram));

				// 作成後ログを出力
				f.log();

				// ----- 関連
				// 関連の属性を確認する
				f.checkRelation();

				// 関連を探索
				for(IPresentation p : classDiagram.getPresentations()){
					f.setRelation(p);
				}

				// 関連を追加or削除
				f.addOrDelRelation();

				// 作成後ログを出力
				f.log();

				TransactionManager.endTransaction();

				// シーケンス図を作成
				if(sequenceDiagramMode){
					if(! f.delRelation) {
						SequenceDiagramWriter.getInstance().createSequenceDiagram(classDiagram, f);
					}
				}

			} catch (Exception e) {
				TransactionManager.abortTransaction();
				logger.log(Level.WARNING, e.getMessage(), e);
			}
		};
	}

	private Point2D getNewClassPosition(IClassDiagram classDiagram) throws InvalidUsingException {
		// 右下にあるNodePresentationの位置
		Point2D rdp = new Point(0, 0);
		for(IPresentation p : classDiagram.getPresentations()){
			// 右下を更新
			if(p instanceof INodePresentation){
				Point2D npp = ((INodePresentation) p).getLocation();
				rdp.setLocation(Math.max(npp.getX(), rdp.getX()), Math.max(npp.getY(), rdp.getY()));
			}
		}
		return new Point2D.Double(rdp.getX() + 100, rdp.getY() + 100);
	}


}
