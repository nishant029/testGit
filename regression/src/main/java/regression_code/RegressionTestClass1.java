package regression_code;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import com.backbase.website.regression.BaseClass;

/**
* @author nishantyadav
*/
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RegressionTestClass1 extends BaseClass {

	@Before
	public void setup() {
		getWebDriver().get("http://computer-database.herokuapp.com/computers");
	}
	
	@Test
	public void addComputer() {
		//code to add computer with BaseClass.java methods
	}
	
	@Test
	public void btestUpdateComputer() {
		//code to update computer with BaseClass.java methods
	}
	
	@Test
	public void ctestComputerPagination() {
		//code to check pagination using BaseClass.java methods
	}
	
	@Test
	public void ztestDeleteComputer() {
		//code to delete computer with BaseClass.java methods
	}
}
