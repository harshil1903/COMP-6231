//package lab1.comp6231.org;

public abstract class BankAccount {

	protected int accNum;
	protected double balance;
	protected static int serialNum = 0;
	
	/** Default constructor 
	 * 
	 */
	public BankAccount()
	{
        // TODO
	    // check the balance
        // check the account Number
		accNum = ++serialNum;
		balance = 0;
	}
	
	/** Overloaded constructor
	 */
	public BankAccount( double startBalance) throws Exception
	{
        // TODO
	    //deposit the balance
        //check the account number
		accNum = ++serialNum;
		if( startBalance >= 0.0 )
			balance = startBalance;

		else
			throw new Exception("Invalid Amount");
	}
	
	/** accessor for balance
	 * 
	 */
	public double getBalance()
	{
        // TODO
        // get the balance
		return balance;
    }
	
	/* accessor for account number
	 * 
	 */
	public int getAccNum()
	{
		return accNum;
	}
	
	/** Deposit amount to account
	 * 
	 */
	public void deposit( double amount ) throws Exception
	{
        // TODO
        // deposit amount of money, if it is legal/valid amount
		if(amount >= 0.0)
			balance += amount;

		else
			throw new Exception("Invalid amount");
	}
	
	/** withdraw amount from account
	 * 
	 */
	public void withdraw( double amount ) throws Exception
	{
		if(amount >= 0.0 && amount <= balance)
			balance -= amount;
		
		else
			throw new Exception("Insufficient Balance");
	}

	/**Override toString()
	 *
	 */
	public String toString()
	{
		// TODO: print the balance amount for specific account type displaying the account number.
		return  "The balance is of the "+ accType() + " " + accNum +  " is " + balance;
	}
	
	public abstract void applyComputation();
	public abstract String accType();
}