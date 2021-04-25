package snytng.astah.plugin.text2model;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.change_vision.jude.api.inf.editor.BasicModelEditor;
import com.change_vision.jude.api.inf.editor.ClassDiagramEditor;
import com.change_vision.jude.api.inf.exception.InvalidEditingException;
import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.exception.ProjectNotFoundException;
import com.change_vision.jude.api.inf.model.IAssociation;
import com.change_vision.jude.api.inf.model.IAttribute;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IDependency;
import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IGeneralization;
import com.change_vision.jude.api.inf.model.IOperation;
import com.change_vision.jude.api.inf.model.IPackage;
import com.change_vision.jude.api.inf.model.IRealization;
import com.change_vision.jude.api.inf.presentation.ILinkPresentation;
import com.change_vision.jude.api.inf.presentation.INodePresentation;
import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.change_vision.jude.api.inf.project.ProjectAccessor;

/**
 * 機能追加クラス
 * @author 0098017
 *
 */
class FunctionCreator {

	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(FunctionCreator.class.getName());
	static {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.CONFIG);
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);
	}

	ProjectAccessor projectAccessor = null;

	IDiagram diagram = null;

	ClassDiagramEditor cde = null;
	BasicModelEditor bme = null;

	String subjectName = null;
	String objectName = null;
	String attributeName = null;
	String operationName = null;

	IClass subjectC = null;
	IClass objectC = null;

	String relationName = null;

	INodePresentation subjectP  = null;
	INodePresentation objectP   = null;

	ILinkPresentation relationP = null;

	IAssociation    relationA   = null;
	IGeneralization relationG   = null;
	IRealization    relationR   = null;
	IDependency     relationD   = null;

	boolean isGeneralization       = false;
	boolean isAggregation          = false;
	boolean isAssociation          = false;
	boolean isAttributeAssociation = false;
	boolean isRealization          = false;
	boolean isDependency           = false;

	IOperation operation = null;
	IAttribute attribute = null;

	boolean addAttribute = false;
	boolean addOperation = false;

	boolean delRelation = false;

	FunctionCreator(ProjectAccessor projectAccessor, IDiagram diagram, boolean addAttribute, boolean addOperation) throws InvalidUsingException, InvalidEditingException {
		this.projectAccessor = projectAccessor;
		this.diagram = diagram;

		this.addAttribute = addAttribute;
		this.addOperation = addOperation;

		// ClassDiagramEditorを取得する。
		this.cde = projectAccessor.getDiagramEditorFactory().getClassDiagramEditor();
		this.cde.setDiagram(diagram);

		// BasicModelEitorを取得する
		this.bme = projectAccessor.getModelEditorFactory().getBasicModelEditor();
	}

	void addSubject(Point2D point) throws ProjectNotFoundException, InvalidEditingException {
		if(subjectP == null){
			subjectC = getIClassFromModel(subjectName);
			subjectP = cde.createNodePresentation(subjectC, point);
		}
	}

	void addObject(Point2D point) throws ProjectNotFoundException, InvalidEditingException{
		if(objectP == null){
			objectC = getIClassFromModel(objectName);
			// 主語と同じ場合にはモデルには追加しない
			if(objectC.equals(subjectC)){
				objectP = subjectP;
			} else {
				objectP = cde.createNodePresentation(objectC, point);
			}
		}
	}

	public void setSubjectOrObject(IPresentation p) {
		if(p.getModel() instanceof IClass){
			IClass c = (IClass)p.getModel();

			if(c.getName().equals(subjectName)){
				subjectC = c;
				subjectP = (INodePresentation)p;
			} else if(c.getName().equals(objectName)){
				objectC = c;
				objectP = (INodePresentation)p;
			}
		}
	}

	void checkRelation(){
		if(attributeName != null && ! attributeName.isEmpty()){
			// の一種だ　→　継承△
			if(attributeName.equals("一種")){
				relationName = "";
				isGeneralization = true;
				logger.log(Level.INFO, () -> "isGeneralization");
			}
			// の実現だ　→　実現->
			else if(attributeName.equals("実現")){
				relationName = "";
				isRealization = true;
				logger.log(Level.INFO, () -> "isRealization");
			}
			// の一部だ　→　集約◇
			else if(attributeName.equals("一部")){
				relationName = "";
				isAggregation = true;
				logger.log(Level.INFO, () -> "isAggregation");
			}
			// それ以外は属性
			else {
				relationName = "の" + attributeName + relationName;
				isAttributeAssociation = true;
				isAssociation = true;
				logger.log(Level.INFO, () -> "isAssociation");
			}
		} else {
			// の一種だ　→　継承△
			if(relationName.equals("の一種だ")){
				relationName = "";
				isGeneralization = true;
				logger.log(Level.INFO, () -> "isGeneralization");
			}
			// の実現だ　→　実現->
			else if(relationName.equals("の実現だ")){
				relationName = "";
				isRealization = true;
				logger.log(Level.INFO, () -> "isRealization");
			}
			// の一部だ　→　集約◇
			else if(relationName.equals("の一部だ")){
				relationName = "";
				isAggregation = true;
				logger.log(Level.INFO, () -> "isAggregation");
			}
			// を使う　→　依存-->
			else if(relationName.equals("を使う")){
				relationName = "";
				isDependency = true;
				logger.log(Level.INFO, () -> "isDependency");
			}
			// それ以外は関連
			else {
				isAssociation = true;
				logger.log(Level.INFO, () -> "isAssociation");
			}
		}
	}

	public void deleteRelation(IPresentation p) throws InvalidEditingException {
		// subjectCとobjectCの間にある関連を削除
		// 名無しの関連を確認
		if(p.getModel() instanceof IAssociation){
			IAssociation a = (IAssociation)p.getModel();
			IAttribute[] attrs = a.getMemberEnds();
			if(a.getName().equals("") &&
					(
							(attrs[0].getType().equals(subjectC) && attrs[1].getType().equals(objectC))
							||
							(attrs[1].getType().equals(subjectC) && attrs[0].getType().equals(objectC))
							)
					){
				relationA = a;
				relationP = (ILinkPresentation)p;
			}
		}

		// 関連があれば削除
		if(relationP != null){
			bme.delete(relationP.getModel());
			// リセット
			relationA = null;
			relationP = null;
		}
	}

	public void setRelation(IPresentation p) {
		// 継承を確認
		if(isGeneralization){
			setRelationOfGeneralization(p);
		}
		// 実現を確認
		else if(isRealization){
			setRelationOfRealization(p);
		}
		// 依存を確認
		else if(isDependency){
			setRelationOfDependency(p);
		}
		// 関連を確認
		else {
			setRelationOfAssociation(p);
		}

	}

	private void setRelationOfAssociation(IPresentation p) {
		if(p.getModel() instanceof IAssociation){
			IAssociation a = (IAssociation)p.getModel();
			IAttribute[] attrs = a.getMemberEnds();

			if(a.getName().equals(relationName) &&
					(
							(attrs[0].getType().equals(subjectC) && attrs[1].getType().equals(objectC))
							||
							(attrs[1].getType().equals(subjectC) && attrs[0].getType().equals(objectC))
							)
					){
				relationA = a;
				relationP = (ILinkPresentation)p;
			}
		}
	}

	private void setRelationOfGeneralization(IPresentation p) {
		if(p.getModel() instanceof IGeneralization){
			IGeneralization g = (IGeneralization)p.getModel();
			if(g.getSuperType().equals(objectC) && g.getSubType().equals(subjectC)){
				relationG = g;
				relationP = (ILinkPresentation)p;
			}
		}
	}

	private void setRelationOfRealization(IPresentation p) {
		if(p.getModel() instanceof IRealization){
			IRealization r = (IRealization)p.getModel();
			if(r.getSupplier().equals(objectC) && r.getClient().equals(subjectC)){
				relationR = r;
				relationP = (ILinkPresentation)p;
			}
		}
	}

	private void setRelationOfDependency(IPresentation p) {
		if(p.getModel() instanceof IDependency){
			IDependency d = (IDependency)p.getModel();
			if(d.getSupplier().equals(objectC) && d.getClient().equals(subjectC)){
				relationD = d;
				relationP = (ILinkPresentation)p;
			}
		}
	}


	// クラス間関連を追加or削除
	public void addOrDelRelation() throws InvalidEditingException {
		if(delRelation) {
			delRelation();
		} else {
			addRelation();
		}
	}


	// クラス間関連を削除
	public void delRelation() throws InvalidEditingException {
		// 継承削除
		if(isGeneralization){
			logger.log(Level.INFO, () -> "isGeneralization");
			delRelationOfGeneralization();
		}
		// 実現削除
		else if(isRealization){
			logger.log(Level.INFO, () -> "isRealization");
			delRelationOfRealization();
		}
		// 依存削除
		else if(isDependency){
			logger.log(Level.INFO, () -> "isDependency");
			delRelationOfDependency();
		}
		// 関連削除
		else {
			logger.log(Level.INFO, () -> "isAssociation");
			delRelationOfAssociation();
		}
	}

	private void delRelationOfAssociation() throws InvalidEditingException {
		Optional<IAssociation> optionalA =
				Arrays.stream(subjectC.getAttributes())
				.map(IAttribute::getAssociation)
				.filter(a -> a.getName().equals(relationName))
				.filter(a ->
				{
					IAttribute[] attrs = a.getMemberEnds();
					return (attrs[0].getType().equals(subjectC) && attrs[1].getType().equals(objectC))
							||
							(attrs[1].getType().equals(subjectC) && attrs[0].getType().equals(objectC));
				})
				.findFirst();

		if(optionalA.isPresent()){
			relationA = optionalA.get();
			bme.delete(relationA);
		}
	}

	private void delRelationOfGeneralization() throws InvalidEditingException {
		Optional<IGeneralization> optionalG =
				Arrays.stream(subjectC.getGeneralizations())
				.filter(g -> g.getSuperType().equals(objectC))
				.findFirst();

		if(optionalG.isPresent()){
			logger.log(Level.INFO, () -> "optionalG is present");
			relationG = optionalG.get();
			bme.delete(relationG);
		} else {
			logger.log(Level.INFO, () -> "optionalG is NOT presenta");
		}
	}

	private void delRelationOfRealization() throws InvalidEditingException {
		Optional<IRealization> optionalR =
				Arrays.stream(subjectC.getClientRealizations())
				.filter(r -> r.getSupplier().equals(objectC))
				.findFirst();

		if(optionalR.isPresent()){
			relationR = optionalR.get();
			bme.delete(relationR);
		}
	}

	private void delRelationOfDependency() throws InvalidEditingException {
		Optional<IDependency> optionalD =
				Arrays.stream(subjectC.getClientDependencies())
				.filter(d -> d.getSupplier().equals(objectC))
				.findFirst();

		if(optionalD.isPresent()){
			relationD = optionalD.get();
			bme.delete(relationD);
		}
	}


	// クラス間関連を追加
	public void addRelation() throws InvalidEditingException {
		// 関連がなければ追加
		if(relationP == null){
			// 継承追加
			if(isGeneralization){
				addRelationOfGeneralization();
			}
			// 実現追加
			else if(isRealization){
				addRelationOfRealization();
			}
			// 依存追加
			else if(isDependency){
				addRelationOfDependency();
			}
			// 関連追加
			else {
				addRelationOfAssociation();
			}
		}

		// 関連に集約を設定
		setAggregation();

		// 関連に属性と操作を追加
		addAttribute();
		addOperation();
	}

	private void addRelationOfAssociation() throws InvalidEditingException {
		Optional<IAssociation> optionalA =
				Arrays.stream(subjectC.getAttributes())
				.map(IAttribute::getAssociation)
				.filter(a -> a.getName().equals(relationName))
				.filter(a ->
				{
					IAttribute[] attrs = a.getMemberEnds();
					return (attrs[0].getType().equals(subjectC) && attrs[1].getType().equals(objectC))
							||
							(attrs[1].getType().equals(subjectC) && attrs[0].getType().equals(objectC));
				})
				.findFirst();

		// 関連を追加
		if(optionalA.isPresent()){
			relationA = optionalA.get();
		} else {
			relationA = bme.createAssociation(
					(IClass)subjectP.getModel(),
					(IClass)objectP.getModel(),
					relationName,
					"",
					"");
		}
		relationP = cde.createLinkPresentation(relationA, subjectP, objectP);
	}

	private void setAggregation() throws InvalidEditingException {
		// 集約を追加
		if(isAggregation){
			List<IAttribute> attrs =
					Arrays.stream(relationA.getMemberEnds())
					.filter(a -> a.getType().equals(objectC))
					.filter(a -> ! a.isAggregate())
					.collect(Collectors.toList());

			for(IAttribute a : attrs){
				a.setAggregation();
			}
		}
	}

	private void addOperation() throws InvalidEditingException {
		if(!addOperation) return;

		// 操作名
		operationName = relationName.startsWith("を") || relationName.startsWith("の") ? relationName.substring(1) : null;
		if(operationName == null){
			logger.log(Level.INFO, () -> "operation:" + operationName + " is invalid method.");
			return;
		}

		// 操作を追加
		Optional<IOperation> optionalOpe =
				Arrays.stream(objectC.getOperations())
				.filter(ope -> ope.getName().equals(operationName))
				.findFirst();

		if(optionalOpe.isPresent()){
			logger.log(Level.INFO, () -> "objectC:" + objectC.getName() + " has already an operation:" + operationName + ".");
		} else {
			operation = bme.createOperation(objectC, operationName, "void");
		}
	}

	private void addAttribute() throws InvalidEditingException {
		if(!(addAttribute && isAttributeAssociation)) return;

		// objectクラスに属性がなければ追加
		Optional<IAttribute> optionalAttr =
				Arrays.stream(objectC.getAttributes())
				.filter(attr -> attr.getName().equals(attributeName))
				.findFirst();

		if(optionalAttr.isPresent()){
			logger.log(Level.INFO, () -> "objectC:" + objectC.getName() + " has an attribute:" + attributeName + ".");
		} else {
			attribute = bme.createAttribute(objectC, attributeName, "int");
		}
	}

	private void addRelationOfGeneralization() throws InvalidEditingException {
		Optional<IGeneralization> optionalG =
				Arrays.stream(subjectC.getGeneralizations())
				.filter(g -> g.getSuperType().equals(objectC))
				.findFirst();

		if(optionalG.isPresent()){
			relationG = optionalG.get();
		} else {
			relationG = bme.createGeneralization(subjectC, objectC, "");
		}
		relationP = cde.createLinkPresentation(relationG, objectP, subjectP);
	}

	private void addRelationOfRealization() throws InvalidEditingException {
		Optional<IRealization> optionalR =
				Arrays.stream(subjectC.getClientRealizations())
				.filter(r -> r.getSupplier().equals(objectC))
				.findFirst();

		if(optionalR.isPresent()){
			relationR = optionalR.get();
		} else {
			if(!Arrays.asList(objectC.getStereotypes()).contains("interface")){
				objectC.addStereotype("interface");
			}
			relationR = bme.createRealization(subjectC, objectC, "");
		}
		relationP = cde.createLinkPresentation(relationR, objectP, subjectP);
	}

	private void addRelationOfDependency() throws InvalidEditingException {
		Optional<IDependency> optionalD =
				Arrays.stream(subjectC.getClientDependencies())
				.filter(d -> d.getSupplier().equals(objectC))
				.findFirst();

		if(optionalD.isPresent()){
			relationD = optionalD.get();
		} else {
			relationD = bme.createDependency(objectC, subjectC, "");
		}
		relationP = cde.createLinkPresentation(relationD, objectP, subjectP);
	}

	void logName(){
		StringBuilder sb = new StringBuilder();
		sb.append("diagram:" + diagram.getName() + System.lineSeparator());
		sb.append("subject:" + subjectName + System.lineSeparator());
		sb.append("object:" + objectName + System.lineSeparator());
		sb.append("relation:" + relationName + System.lineSeparator());
		logger.log(Level.INFO, sb::toString);
	}

	void log(){
		StringBuilder sb = new StringBuilder();
		sb.append("diagram:" + diagram.getName() + System.lineSeparator());
		sb.append("subjectC:" + subjectC.getName() + ", subjectP:" + subjectP.getLabel() + System.lineSeparator());
		sb.append("objectC:"  + objectC.getName()  + ", objectP:"  + objectP.getLabel()  + System.lineSeparator());
		if(relationA != null){
			sb.append("relationA:" + relationA.getName() + ", relationP:" + relationP.getLabel() + System.lineSeparator());
		}
		if(relationG != null){
			sb.append("relationG:" + relationG.getName() + ", relationP:" + relationP.getLabel() + System.lineSeparator());
		}
		if(relationR != null){
			sb.append("relationR:" + relationR.getName() + ", relationP:" + relationP.getLabel() + System.lineSeparator());
		}
		if(relationD != null){
			sb.append("relationD:" + relationD.getName() + ", relationP:" + relationP.getLabel() + System.lineSeparator());
		}
		logger.log(Level.INFO, sb::toString);
	}

	IClass getIClassFromModel(String subjectName)
			throws ProjectNotFoundException, InvalidEditingException {
		Optional<IClass> optionalC =
				Arrays.stream(projectAccessor.findElements(IClass.class, subjectName))
				.map(IClass.class::cast)
				// クラス図と同じレイヤにクラスがあるかどうか確認する
				.filter(cl -> diagram.getContainer() == cl.getContainer())
				.findFirst();

		// 既存クラスがなければ作る
		if(optionalC.isPresent()){
			return optionalC.get();
		} else {
			return bme.createClass((IPackage)diagram.getOwner(),subjectName);
		}
	}

}

