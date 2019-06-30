public class Pcb {
    private String id;          // Identificador del proceso
    private char estado;        // Estado del proceso R de restante y F de finalizado
    private int registro[];     // Guarda el estado de los 32 registros del núcleo
    private int pc;             // Guarda el valor del PC
    private int ir;             // Guarda el valor del IR
    private int ciclosReloj;    // Guarda la cantidad de ciclos de reloj ocupados por el proceso para acabar


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

    /**
     * Imprime en pantalla el valor de los registros y el PC de este PCB
     */
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
