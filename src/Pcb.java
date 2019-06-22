public class Pcb {
    private String id;
    private char estado;
    private int registro[];
    private int pc;
    private int ir;
    private int ciclosReloj;


    public Pcb(String id, int pc){
        this.id = id;
        this.pc = pc;
        estado = 'R';
        registro = new int[32];

    }

    public void setEstado(char estado) {
        this.estado = estado;
    }

    public void setRegistro(int[] registro) {
        for(int i = 0; i < registro.length; ++i)
            this.registro[i] = registro[i];
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public void setIr(int ir) {
        this.ir = ir;
    }

    public void setCiclosReloj(int ciclosReloj) {
        this.ciclosReloj = ciclosReloj;
    }

    public String getId() {
        return id;
    }

    public char getEstado() {
        return estado;
    }

    public int[] getRegistro() {
        return registro;
    }

    public int getPc() {
        return pc;
    }

    public int getIr() {
        return ir;
    }

    public int getCiclosReloj() {
        return ciclosReloj;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void print(){
        System.out.println("\n******************************************************************************");
        System.out.println("\tEl hilillo " + this.id + " Tiene los siguientes valores en sus registros");
        System.out.print("\t");

        for(int i = 0; i < registro.length; ++i)
            System.out.print(registro[i] + " ");

        System.out.println("PC " + this.pc);

        System.out.println("******************************************************************************");
    }
}
