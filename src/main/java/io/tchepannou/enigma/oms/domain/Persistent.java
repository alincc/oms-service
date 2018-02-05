package io.tchepannou.enigma.oms.domain;

public abstract class Persistent {
    public abstract Integer getId();

    @Override
    public boolean equals(final Object obj) {

        if (getClass().equals(obj.getClass())){
            final Integer id = getId();
            if (id == null){
                return false;
            } else {
                return id.equals(((Persistent)obj).getId());
            }
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        final Integer id = getId();
        return id == null ? super.hashCode() : id.hashCode();
    }
}
