public static boolean isValid(int[][] board, int r, int c, int num,
                              List<Inequality> inequalities) {
    int n = board.length;

    for (int i = 0; i < n; i++) {
        if (i != c && board[r][i] == num) return false;
        if (i != r && board[i][c] == num) return false;
    }

    for (Inequality ineq : inequalities) {
        boolean touchesFirst = ineq.getR1() == r && ineq.getC1() == c;
        boolean touchesSecond = ineq.getR2() == r && ineq.getC2() == c;

        if (!touchesFirst && !touchesSecond) continue;

        int a = touchesFirst ? num : board[ineq.getR1()][ineq.getC1()];
        int b = touchesSecond ? num : board[ineq.getR2()][ineq.getC2()];

        if (a == 0 || b == 0) continue;

        if (ineq.getDir() == '<') {
            if (!(a < b)) return false;
        } else {
            if (!(a > b)) return false;
        }
    }

    return true;
}