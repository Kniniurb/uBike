////////////////////////////////////////////////////////////////////////////////
// The following FIT Protocol software provided may be used with FIT protocol
// devices only and remains the copyrighted property of Dynastream Innovations Inc.
// The software is being provided on an "as-is" basis and as an accommodation,
// and therefore all warranties, representations, or guarantees of any kind
// (whether express, implied or statutory) including, without limitation,
// warranties of merchantability, non-infringement, or fitness for a particular
// purpose, are specifically disclaimed.
//
// Copyright 2013 Dynastream Innovations Inc.
////////////////////////////////////////////////////////////////////////////////
// ****WARNING****  This file is auto-generated!  Do NOT edit this file.
// Profile Version = 6.10Release
// Tag = $Name:  $
////////////////////////////////////////////////////////////////////////////////


package com.garmin.fit;


public class SportMesg extends Mesg {

   protected static final	Mesg sportMesg;
   static {         
      // sport   
      sportMesg = new Mesg("sport", MesgNum.SPORT);
      sportMesg.addField(new Field("sport", 0, 0, 1, 0, ""));
      
      sportMesg.addField(new Field("sub_sport", 1, 0, 1, 0, ""));
      
      sportMesg.addField(new Field("name", 3, 7, 1, 0, ""));
      
   }

   public SportMesg() {
      super(Factory.createMesg(MesgNum.SPORT));
   }

   public SportMesg(final Mesg mesg) {
      super(mesg);
   }


   /**
    * Get sport field
    *
    * @return sport
    */
   public Sport getSport() {
      Short value = getFieldShortValue(0, 0, Fit.SUBFIELD_INDEX_MAIN_FIELD);
      if (value == null)
         return null;
      return Sport.getByValue(value);
   }

   /**
    * Set sport field
    *
    * @param sport
    */
   public void setSport(Sport sport) {
      setFieldValue(0, 0, sport.value, Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

   /**
    * Get sub_sport field
    *
    * @return sub_sport
    */
   public SubSport getSubSport() {
      Short value = getFieldShortValue(1, 0, Fit.SUBFIELD_INDEX_MAIN_FIELD);
      if (value == null)
         return null;
      return SubSport.getByValue(value);
   }

   /**
    * Set sub_sport field
    *
    * @param subSport
    */
   public void setSubSport(SubSport subSport) {
      setFieldValue(1, 0, subSport.value, Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

   /**
    * Get name field
    *
    * @return name
    */
   public String getName() {
      return getFieldStringValue(3, 0, Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

   /**
    * Set name field
    *
    * @param name
    */
   public void setName(String name) {
      setFieldValue(3, 0, name, Fit.SUBFIELD_INDEX_MAIN_FIELD);
   }

}
