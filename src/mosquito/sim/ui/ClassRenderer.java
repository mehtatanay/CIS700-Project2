package mosquito.sim.ui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import mosquito.sim.Player;

public class ClassRenderer extends DefaultListCellRenderer
{
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		try
		{
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof Class)
			{
				Class<Player> p = (Class<Player>) value;
				Player pl = ((Player) p.newInstance());
				String strPlayer = pl.getClass().getName(); //pl.getName();
				strPlayer = strPlayer.replace("mosquito.", " ");
				strPlayer = strPlayer.replace(".", " ");
				//strPlayer = pl.getClass().getPackage().getName().replace("mosquito.", "") + " "+ strPlayer;
				if (strPlayer != null)
				{
					setText(strPlayer);
				}
			}
		} catch (Exception e)
		{

		}
		return this;
	}

}
