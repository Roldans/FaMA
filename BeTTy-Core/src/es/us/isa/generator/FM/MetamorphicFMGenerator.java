/**
 * 	This file is part of Betty.
 *
 *     Betty is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Betty is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with Betty.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.us.isa.generator.FM;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import es.us.isa.FAMA.models.FAMAfeatureModel.FAMAFeatureModel;
import es.us.isa.FAMA.models.FAMAfeatureModel.Feature;
import es.us.isa.FAMA.models.featureModel.GenericFeature;
import es.us.isa.FAMA.models.featureModel.Product;
import es.us.isa.FAMA.models.variabilityModel.VariabilityModel;
import es.us.isa.generator.Characteristics;
import es.us.isa.generator.IGenerator;
import es.us.isa.utils.BettyException;
import es.us.isa.utils.BoundedArrayList;
import es.us.isa.utils.CombinationGenerator;

/**
 * This class implements a decorator for random FM generators. When this decorator is applied, the set of products of the random feature model
 * being generated is also computed. The products are generated by using so-called metamorphic relations. Once generated,
 * the model and its products can be used for automated functional testing of feature model analysis tools. For more information, visit the 
 * BeTTy Website.
 * 
 */
public class MetamorphicFMGenerator extends AbstractFMGeneratorDecorator  {

	/**
	 * This variable stores the number of products represented by the model.
	 * This is updated at each step during the generation process using
	 * so-called metamorphic relations. For more information, please go the
	 * BeTTy Website.
	 */
	private long calculatedNumberOfProducts = 0;

	/**
	 * The collection that stores the generated Products.
	 */
	protected ArrayList<Product> products;

	/**
	 * Maximum number of products represented by the model generated (if this
	 * number is reached the program is stopped to avoid memory overflows)
	 */
	private int maxProducts;


	/**
	 * These are the user's preferences for the generation (e.g. percentage of
	 * mandatory features) .
	 */
	private int MAX_ATTEMPTS = 1000; 			// Maximum number of attempts before aborting the generation. This may happen if the input constraints are too hard.
	
	
	public MetamorphicFMGenerator(IGenerator gen) {
		super(gen);
	}
	
	
	/**
	 * This is a hook method. It is call whenever the decorated object call the resetGenerator method.
	 */
	public void updateResetGenerator(Characteristics c) {
		
		GeneratorCharacteristics ch = (GeneratorCharacteristics) c;
		
		if (ch.getMaxProducts() != -1) {
			this.maxProducts = (int) ch.getMaxProducts();
			this.products = new BoundedArrayList<Product>(maxProducts);
			this.calculatedNumberOfProducts=0;
		}
	}
	
	/**
	 * This is a hook method. It is call whenever the decorated object call the addRoot method.
	 */
	public void updateRoot(FAMAFeatureModel fm, Feature root) {

		// Update products
		Product p = new Product();
		p.addFeature(root);
		products.add(p);
		this.calculatedNumberOfProducts = 1;
	}
	

	/**
	 * This is a hook method. It is call whenever the decorated object call the addMandatory method.
	 */
	public void updateMandatory(Feature parent, Feature child) {

		// Update products
		Iterator<Product> itp = products.iterator();
		while (itp.hasNext()) {
			Product p = itp.next();
			if (p.getFeatures().contains(parent))
				p.addFeature(child);
		}
		
	}
	
	/**
	 * This is a hook method. It is call whenever the decorated object call the addOptional method.
	 */
	public void updateOptional(Feature parent, Feature child) {

		// Update products
		int parentProducts = 0; // Number of products containing the parent
								// feature
		// Update set of products
		List<Product> tmpProducts = new BoundedArrayList<Product>(this.maxProducts);
		Iterator<Product> itp = products.iterator();
		while (itp.hasNext()) {
			Product p = itp.next();
			if (p.getFeatures().contains(parent)) {
				Product np = cloneProduct(p);
				np.addFeature(child);
				tmpProducts.add(np);
				parentProducts++;
			}
		}

		products.addAll(tmpProducts);

		// Update the number of products
		this.calculatedNumberOfProducts += parentProducts;
		
	}

	/**
	 * This is a hook method. It is call whenever the decorated object call the addAlternative method.
	 */
	public void updateAlternative(Feature parent, List<Feature> children) {
	
		// Update set of products
		int parentProducts = 0;
		List<Product> tmpProducts = new BoundedArrayList<Product>(maxProducts);
		Iterator<Product> itp = products.iterator();
		while (itp.hasNext()) {
			Product p = itp.next();
			if (p.getFeatures().contains(parent)) {
				for (int i = 1; i < children.size(); i++) {
					Product np = cloneProduct(p);
					np.addFeature(children.get(i));
					tmpProducts.add(np);
				}
				p.addFeature(children.get(0));
				parentProducts++;
			}
		}

		products.addAll(tmpProducts);

		// Update number of products
		this.calculatedNumberOfProducts += (children.size() - 1)
				* parentProducts;

	}
	
	/**
	 * This is a hook method. It is call whenever the decorated object call the addOr method.
	 */
	public void updateOr(Feature parent, List<Feature> children) {

		// Update products

		int parentProducts = 0;
		List<Product> tmpProducts = new BoundedArrayList<Product>(maxProducts);
		Iterator<Product> itp = products.iterator();
		while (itp.hasNext()) {
			Product p = itp.next();
			if (p.getFeatures().contains(parent)) {
				// Combinations of one element
				for (int i = 1; i < children.size(); i++) {
					Product np = cloneProduct(p);
					np.addFeature(children.get(i));
					tmpProducts.add(np);
				}

				// Combinations of n elements
				for (int i = 2; i <= children.size(); i++) {
					CombinationGenerator gen = new CombinationGenerator(
							children.size(), i);
					while (gen.hasMore()) {
						int[] indices = gen.getNext();
						Product np = cloneProduct(p);
						for (int j = 0; j < indices.length; j++)
							np.addFeature(children.get(indices[j]));
						tmpProducts.add(np);
					}
				}

				p.addFeature(children.get(0));
				parentProducts++;
			}
		}

		products.addAll(tmpProducts);

		// Update number of products
		this.calculatedNumberOfProducts += (Math.pow(2, children.size()) - 2)
				* parentProducts;

	}
	
	/**
	 * This is a hook method. It is call whenever the decorated object call the addExcludes method.
	 */
	public void updateExcludes(FAMAFeatureModel fm, Feature origin, Feature destination){
		// Update products

		int productsRemoved = 0;
		Iterator<Product> itp = products.iterator();
		while (itp.hasNext()) {
			Product p = itp.next();
			if (p.getFeatures().contains(origin)
					&& p.getFeatures().contains(destination)) {
				itp.remove();
				productsRemoved++;
			}
		}

		// Update number of products
		this.calculatedNumberOfProducts -= productsRemoved;
	}
	
	/**
	 * This is a hook method. It is call whenever the decorated object call the addRequires method.
	 */
	public void updateRequires(FAMAFeatureModel fm, Feature origin, Feature destination) {

		// Update products
		int productsRemoved = 0;
		Iterator<Product> itp = products.iterator();
		while (itp.hasNext()) {
			Product p = itp.next();
			if (p.getFeatures().contains(origin)
					&& !p.getFeatures().contains(destination)) {
				itp.remove();
				productsRemoved++;
			}
		}

		// Update number of products
		this.calculatedNumberOfProducts -= productsRemoved;
	}
	
	
	/**
	 * Return a feature model with the characteristics received as input.
	 * 
	 * @param ch User's preferences for the generation.
	 *            
	 * @return the feature model generated. 
	 */
	@Override
	public VariabilityModel generateFM(Characteristics ch)  {
		GeneratorCharacteristics gch = ((GeneratorCharacteristics) ch).clone();	

		System.out.println("The generation of the set of products may take several minutes...");
		
		FAMAFeatureModel model = null;
		int i=0;
		Random random = new Random();
		while(i<MAX_ATTEMPTS&&model==null){
				try{
					model=(FAMAFeatureModel) generate(gch);
				}catch (ArrayIndexOutOfBoundsException e) {
					long seed = gch.getSeed();
					gch = ((GeneratorCharacteristics) ch).clone();	
					gch.setSeed(seed + random.nextInt());
					i++;
				}
		}
	
		if (i == MAX_ATTEMPTS)
			throw new BettyException("Too many generation attempts. Try to relax the input constraints");
		
		System.out.println("Number of tries: " + i);
		
		return model;
	}
	

	private VariabilityModel generate(Characteristics ch)  {
		super.resetGenerator(ch);
			
		return (FAMAFeatureModel) super.generateFM(ch);
	}
	
	/**
	 * This method is used in the generation of the number of products
	 * 
	 * @param product
	 *            The product to be cloned
	 * @return The coned Product
	 */
	private Product cloneProduct(Product product) {

		Product np = new Product();
		Iterator<GenericFeature> itf = product.getFeatures().iterator();
		while (itf.hasNext())
			np.addFeature(itf.next());

		return np;
	}
	
	/**
	 * 
	 * @return The set of products of the feature model generated.
	 */
	public List<Product> getPoducts() {
		return this.products;
	}

	/**
	 * 
	 * @return the number of products of the model generated.
	 */
	public double getNumberOfProducts() {
		return calculatedNumberOfProducts;
	}

}