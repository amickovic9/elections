package rs.raf.pds.faulttolerance.z1;


public class AccountServiceImpl {

	float amount = 0.0f;

	
	public AccountServiceImpl() {
		
	}
	/**
	 * Ovakva funkcija se zove nekada depositAmount
	 * @param value
	 * @param applyLog
	 * @return
	 */
	public synchronized float addAmount(float value) {
		
		amount += value;
			
		return amount;
	}
	public synchronized float witdrawAmount(float value) {
			
		if (amount>=value) {
			
			amount -= value;
			
			return amount;
			
		}else
		{
			return -1;
			
		}

	}
	
	
	public synchronized float getAmount() {
		return amount;
	}
		
}
