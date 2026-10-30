const farmPhoto = "https://images.unsplash.com/photo-1783605021867-c2f58de01b8a?auto=format&fit=crop&crop=entropy&w=240&h=240&q=85";

function FarmConnectIcon({ className = "" }) {
  return <img className={className} src={farmPhoto} alt="" aria-hidden="true" />;
}

export default FarmConnectIcon;
