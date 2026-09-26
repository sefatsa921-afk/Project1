public class CheckWin{

		private boolean checkWinCondition(){
		// checking if the entire board is filled
		int size=board.length;
		for(int r=0;r<size;r++){
			for(int c=0;c<size;c++){
				if(board[r][c].getValue()==0){// the 0 will depend on what we use to represent the empty box
					return false;
				}
			}
		}
		// checking for duplicates
		for(int i=0;i<size;i++){
			boolean[] rowthere=new boolean[size+1];
			boolean[] colthere=new boolean[size+1];
			for(int k=0;k<size;k++){
				int rowValu=board[i][k].getValue();
				int colValu=board[k][i].getValue();

				if(rowthere[rowValu] || colthere[colValu]){
					return false;
				}
				rowthere[rowValu]=true;
				colthere[colValu]=true;
			}
		}
		// verifying the constraints
		for(int i=0;i<constraints.size();i++){
			Constraint current=constraints.get(i);
			if(!current.isSatisfied(board)){
				return false;
			}
		}
	return true;  	//if all checks passed
	}
}