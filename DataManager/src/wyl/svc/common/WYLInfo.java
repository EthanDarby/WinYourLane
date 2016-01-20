package wyl.svc.common;

public class WYLInfo {

	int season_number;
	String patch;
	double numberOfMatches;
	double b_ratio, s_ratio, g_ratio, p_ratio, d_ratio, m_ratio, c_ratio;
	
	public WYLInfo(){
		this.season_number = 0;
		this.patch = "0.00";
		this.numberOfMatches = 0;
		this.b_ratio = 0;
		this.s_ratio = 0;
		this.g_ratio = 0;
		this.p_ratio = 0;
	}
}
